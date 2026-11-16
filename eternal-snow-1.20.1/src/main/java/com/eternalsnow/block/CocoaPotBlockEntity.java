package com.eternalsnow.block;

import com.eternalsnow.menu.CocoaPotMenu;
import com.eternalsnow.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class CocoaPotBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int DRINK_NONE = 0;
    public static final int DRINK_COCOA = 1;
    public static final int DRINK_CARAMEL = 2;

    public static final int MAX_SERVINGS = 3;
    public static final int COOK_TIME_TICKS = 100; // 5.0 seconds on a lit furnace!

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int drinkType = DRINK_NONE;
    private int servings = 0;
    private int cookProgress = 0;
    private boolean heatedBelow = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CocoaPotBlockEntity.this.drinkType;
                case 1 -> CocoaPotBlockEntity.this.servings;
                case 2 -> CocoaPotBlockEntity.this.cookProgress;
                case 3 -> CocoaPotBlockEntity.this.heatedBelow ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CocoaPotBlockEntity.this.drinkType = value;
                case 1 -> CocoaPotBlockEntity.this.servings = value;
                case 2 -> CocoaPotBlockEntity.this.cookProgress = value;
                case 3 -> CocoaPotBlockEntity.this.heatedBelow = (value != 0);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CocoaPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.COCOA_POT_BE.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public int getDrinkType() {
        return this.drinkType;
    }

    public int getServings() {
        return this.servings;
    }

    public int getCookProgress() {
        return this.cookProgress;
    }

    /**
     * Exact user rule: show the "full" bone inside the 3D pot whenever something is inside/cooking OR cooked;
     * hide the "full" bone completely when empty!
     */
    public boolean shouldShowFullBone() {
        return this.servings > 0 || this.cookProgress > 0;
    }

    /**
     * Checks if the pot sits directly on top of a working (LIT) furnace / smoker / blast furnace / 3D Fireplace!
     */
    public static boolean isLitFurnaceBelow(Level level, BlockPos potPos) {
        BlockPos belowPos = potPos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.getBlock() instanceof AbstractFurnaceBlock) {
            return below.hasProperty(AbstractFurnaceBlock.LIT) && below.getValue(AbstractFurnaceBlock.LIT);
        }
        if (below.getBlock() instanceof FireplaceBlock) {
            return below.getValue(FireplaceBlock.LIT);
        }
        if (below.getBlock() instanceof FireplacePartBlock) {
            BlockPos master = FireplacePartBlock.getMasterPos(belowPos, below);
            BlockState ms = level.getBlockState(master);
            return ms.getBlock() instanceof FireplaceBlock && ms.getValue(FireplaceBlock.LIT);
        }
        // Support modded furnaces/stoves that use BlockStateProperties.LIT
        if (below.hasProperty(BlockStateProperties.LIT) && below.getValue(BlockStateProperties.LIT)) {
            String id = below.getBlock().getDescriptionId();
            return id.contains("furnace") || id.contains("smoker") || id.contains("stove");
        }
        return false;
    }

    private int matchValidRecipeDrink() {
        ItemStack milkSlot = this.items.get(0);
        ItemStack waterSlot = this.items.get(1);
        ItemStack flavorSlot = this.items.get(2);

        // Allow Milk & Water in either slot 0/1 so players don't even have to guess left vs middle!
        boolean hasMilkAndWater =
                (milkSlot.is(Items.MILK_BUCKET) && waterSlot.is(Items.WATER_BUCKET))
                || (milkSlot.is(Items.WATER_BUCKET) && waterSlot.is(Items.MILK_BUCKET));

        if (!hasMilkAndWater) {
            return DRINK_NONE;
        }
        if (flavorSlot.is(Items.COCOA_BEANS) && flavorSlot.getCount() >= 4) {
            return DRINK_COCOA;
        }
        if (ModRegistry.CARAMEL.isPresent() && flavorSlot.is(ModRegistry.CARAMEL.get()) && flavorSlot.getCount() >= 4) {
            return DRINK_CARAMEL;
        }
        return DRINK_NONE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CocoaPotBlockEntity be) {
        boolean wasShowingFull = be.shouldShowFullBone();
        int prevDrink = be.drinkType;

        be.heatedBelow = isLitFurnaceBelow(level, pos);

        if (be.servings == 0) {
            int recipeDrink = be.matchValidRecipeDrink();
            if (be.heatedBelow && recipeDrink != DRINK_NONE) {
                be.drinkType = recipeDrink;
                be.cookProgress++;

                if (level instanceof ServerLevel sl && level.getGameTime() % 12L == 0L) {
                    sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D, 1, 0.12D, 0.04D, 0.12D, 0.01D);
                }

                if (be.cookProgress >= COOK_TIME_TICKS) {
                    // Replace Milk Bucket & Water Bucket with Empty Buckets (buckets stay!), consume 4 cocoa/caramel!
                    be.items.set(0, new ItemStack(Items.BUCKET));
                    be.items.set(1, new ItemStack(Items.BUCKET));
                    ItemStack flavor = be.items.get(2);
                    flavor.shrink(4);
                    if (flavor.isEmpty()) {
                        be.items.set(2, ItemStack.EMPTY);
                    }

                    be.drinkType = recipeDrink;
                    be.servings = MAX_SERVINGS;
                    be.cookProgress = 0;
                    be.setChanged();
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.85F, 1.05F);
                    be.syncToClients();
                    return;
                }
                be.setChanged();
            } else {
                if (be.cookProgress > 0) {
                    be.cookProgress = Math.max(0, be.cookProgress - 2);
                    if (be.cookProgress == 0) {
                        be.drinkType = DRINK_NONE;
                    }
                    be.setChanged();
                }
            }
        } else {
            // Gentle steam above cooked hot drink when sitting on warm furnace
            if (be.heatedBelow && level instanceof ServerLevel sl && level.getGameTime() % 25L == 0L) {
                sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.52D, pos.getZ() + 0.5D, 1, 0.10D, 0.03D, 0.10D, 0.006D);
            }
        }

        if (wasShowingFull != be.shouldShowFullBone() || prevDrink != be.drinkType) {
            be.syncToClients();
        }
    }

    public int takeOneServing() {
        if (this.servings <= 0 || this.drinkType == DRINK_NONE) {
            return DRINK_NONE;
        }
        int takenDrink = this.drinkType;
        this.servings--;
        if (this.servings <= 0) {
            this.servings = 0;
            this.drinkType = DRINK_NONE;
        }
        this.setChanged();
        this.syncToClients();
        return takenDrink;
    }

    public void syncToClients() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState st = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, st, st, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.eternalsnow.cocoa_pot");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new CocoaPotMenu(containerId, playerInv, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack st : this.items) {
            if (!st.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(this.items, slot, amount);
        if (!res.isEmpty()) {
            this.setChanged();
        }
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("DrinkType", this.drinkType);
        tag.putInt("Servings", this.servings);
        tag.putInt("CookProgress", this.cookProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        this.drinkType = tag.getInt("DrinkType");
        this.servings = tag.getInt("Servings");
        this.cookProgress = tag.getInt("CookProgress");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("DrinkType", this.drinkType);
        tag.putInt("Servings", this.servings);
        tag.putInt("CookProgress", this.cookProgress);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
