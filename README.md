---- Minecraft Crash Report ----
// Shall we play a game?

Time: 2026-08-19 04:15:37
Description: Rendering overlay

java.lang.IllegalArgumentException: Failed to create model for fly_drone:fpv_drone
	at net.minecraft.client.renderer.entity.EntityRenderers.m_257087_(EntityRenderers.java:164) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at java.util.HashMap.forEach(Unknown Source) ~[?:?] {}
	at net.minecraft.client.renderer.entity.EntityRenderers.m_174049_(EntityRenderers.java:160) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.renderer.entity.EntityRenderDispatcher.m_6213_(EntityRenderDispatcher.java:360) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.server.packs.resources.ResourceManagerReloadListener.m_10759_(ResourceManagerReloadListener.java:15) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at java.util.concurrent.CompletableFuture$UniRun.tryFire(Unknown Source) ~[?:?] {}
	at java.util.concurrent.CompletableFuture$Completion.run(Unknown Source) ~[?:?] {}
	at net.minecraft.server.packs.resources.SimpleReloadInstance.m_143940_(SimpleReloadInstance.java:69) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.m_6367_(BlockableEventLoop.java:156) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.util.thread.ReentrantBlockableEventLoop.m_6367_(ReentrantBlockableEventLoop.java:23) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.m_7245_(BlockableEventLoop.java:130) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.util.thread.BlockableEventLoop.m_18699_(BlockableEventLoop.java:115) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.Minecraft.m_91383_(Minecraft.java:1106) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.Minecraft.m_91374_(Minecraft.java:718) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.main.Main.main(Main.java:218) ~[1.20.1-47.4.20.jar:?] {re:classloading}
	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?] {}
	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source) ~[?:?] {}
	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?] {}
	at java.lang.reflect.Method.invoke(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:111) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at net.minecraftforge.fml.loading.targets.CommonLaunchHandler.clientService(CommonLaunchHandler.java:99) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at net.minecraftforge.fml.loading.targets.CommonClientLaunchHandler.lambda$makeService$0(CommonClientLaunchHandler.java:25) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.Launcher.run(Launcher.java:108) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.Launcher.main(Launcher.java:78) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:141) ~[bootstraplauncher-1.1.2.jar:?] {}
Caused by: java.util.NoSuchElementException: Can't find part propeller_lb
	at net.minecraft.client.model.geom.ModelPart.m_171324_(ModelPart.java:91) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at com.zzynes.fly_drone.FPVDroneModel.<init>(FPVDroneModel.java:26) ~[fly_drone-1.0.0.jar%23157!/:1.0.0] {re:classloading}
	at com.zzynes.fly_drone.FPVDroneRenderer.<init>(FPVDroneRenderer.java:12) ~[fly_drone-1.0.0.jar%23157!/:1.0.0] {re:classloading}
	at net.minecraft.client.renderer.entity.EntityRenderers.m_257087_(EntityRenderers.java:162) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	... 29 more


A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Render thread
Suspected Mod: 
	Fly Drone (fly_drone), Version: 1.0.0
		at TRANSFORMER/fly_drone@1.0.0/com.zzynes.fly_drone.FPVDroneModel.<init>(FPVDroneModel.java:26)
Stacktrace:
	at net.minecraft.client.renderer.entity.EntityRenderers.m_257087_(EntityRenderers.java:164) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at java.util.HashMap.forEach(Unknown Source) ~[?:?] {}
	at net.minecraft.client.renderer.entity.EntityRenderers.m_174049_(EntityRenderers.java:160) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.renderer.entity.EntityRenderDispatcher.m_6213_(EntityRenderDispatcher.java:360) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.server.packs.resources.ResourceManagerReloadListener.m_10759_(ResourceManagerReloadListener.java:15) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at java.util.concurrent.CompletableFuture$UniRun.tryFire(Unknown Source) ~[?:?] {}
	at java.util.concurrent.CompletableFuture$Completion.run(Unknown Source) ~[?:?] {}
	at net.minecraft.server.packs.resources.SimpleReloadInstance.m_143940_(SimpleReloadInstance.java:69) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.m_6367_(BlockableEventLoop.java:156) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.util.thread.ReentrantBlockableEventLoop.m_6367_(ReentrantBlockableEventLoop.java:23) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.m_7245_(BlockableEventLoop.java:130) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
-- Overlay render details --
Details:
	Overlay name: net.minecraftforge.client.loading.ForgeLoadingOverlay
Stacktrace:
	at net.minecraft.client.renderer.GameRenderer.m_109093_(GameRenderer.java:957) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.Minecraft.m_91383_(Minecraft.java:1146) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.Minecraft.m_91374_(Minecraft.java:718) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.main.Main.main(Main.java:218) ~[1.20.1-47.4.20.jar:?] {re:classloading}
	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?] {}
	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source) ~[?:?] {}
	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[?:?] {}
	at java.lang.reflect.Method.invoke(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:111) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at net.minecraftforge.fml.loading.targets.CommonLaunchHandler.clientService(CommonLaunchHandler.java:99) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at net.minecraftforge.fml.loading.targets.CommonClientLaunchHandler.lambda$makeService$0(CommonClientLaunchHandler.java:25) ~[fmlloader-1.20.1-47.4.20.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.Launcher.run(Launcher.java:108) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.Launcher.main(Launcher.java:78) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) ~[modlauncher-10.0.9.jar:?] {}
	at cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:141) ~[bootstraplauncher-1.1.2.jar:?] {}


-- Last reload --
Details:
	Reload number: 1
	Reload reason: initial
	Finished: No
	Packs: vanilla, mod_resources

-- System Details --
Details:
	Minecraft Version: 1.20.1
	Minecraft Version ID: 1.20.1
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 17.0.18, Azul Systems, Inc.
	Java VM Version: OpenJDK 64-Bit Server VM (mixed mode, sharing), Azul Systems, Inc.
	Memory: 461198568 bytes (439 MiB) / 742391808 bytes (708 MiB) up to 8388608000 bytes (8000 MiB)
	CPUs: 24
	Processor Vendor: GenuineIntel
	Processor Name: Intel(R) Xeon(R) CPU E5-2680 v3 @ 2.50GHz
	Identifier: Intel64 Family 6 Model 63 Stepping 2
	Microarchitecture: Haswell (Server)
	Frequency (GHz): 2.49
	Number of physical packages: 1
	Number of physical CPUs: 12
	Number of logical CPUs: 24
	Graphics card #0 name: AMD Radeon RX 580 2048SP
	Graphics card #0 vendor: Advanced Micro Devices, Inc. (0x1002)
	Graphics card #0 VRAM (MB): 4095.00
	Graphics card #0 deviceId: 0x6fdf
	Graphics card #0 versionInfo: DriverVersion=31.0.21924.61
	Memory slot #0 capacity (MB): 16384.00
	Memory slot #0 clockSpeed (GHz): 2.40
	Memory slot #0 type: DDR4
	Memory slot #1 capacity (MB): 16384.00
	Memory slot #1 clockSpeed (GHz): 2.40
	Memory slot #1 type: DDR4
	Virtual memory max (MB): 37473.13
	Virtual memory used (MB): 15968.37
	Swap memory total (MB): 4864.00
	Swap memory used (MB): 0.00
	JVM Flags: 2 total; -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Xmx8000M
	Launched Version: 1.20.1
	Backend library: LWJGL version 3.3.1 build 7
	Backend API: AMD Radeon RX 580 2048SP GL version 4.6.0 Core Profile Context 26.1.1.251211, ATI Technologies Inc.
	Window size: 1920x1080
	GL Caps: Using framebuffer using OpenGL 3.2
	GL debug messages: 
	Using VBOs: Yes
	Is Modded: Definitely; Client brand changed to 'forge'
	Type: Client (map_client.txt)
	Graphics mode: fancy
	Resource Packs: 
	Current Language: ru_ru
	CPU: 24x Intel(R) Xeon(R) CPU E5-2680 v3 @ 2.50GHz
	ModLauncher: 10.0.9+10.0.9+main.dcd20f30
	ModLauncher launch target: forgeclient
	ModLauncher naming: srg
	ModLauncher services: 
		mixin-0.8.5.jar mixin PLUGINSERVICE 
		eventbus-6.2.33.jar eventbus PLUGINSERVICE 
		fmlloader-1.20.1-47.4.20.jar slf4jfixer PLUGINSERVICE 
		fmlloader-1.20.1-47.4.20.jar object_holder_definalize PLUGINSERVICE 
		fmlloader-1.20.1-47.4.20.jar runtime_enum_extender PLUGINSERVICE 
		fmlloader-1.20.1-47.4.20.jar capability_token_subclass PLUGINSERVICE 
		accesstransformers-8.0.4.jar accesstransformer PLUGINSERVICE 
		fmlloader-1.20.1-47.4.20.jar runtimedistcleaner PLUGINSERVICE 
		modlauncher-10.0.9.jar mixin TRANSFORMATIONSERVICE 
		modlauncher-10.0.9.jar fml TRANSFORMATIONSERVICE 
	FML Language Providers: 
		minecraft@1.0
		lowcodefml@{MINECRAFT_ACCESS_TOKEN}
		javafml@{MINECRAFT_ACCESS_TOKEN}
	Mod List: 
		client-1.20.1-20230612.114412-srg.jar             |Minecraft                     |minecraft                     |1.20.1              |DONE      |Manifest: a1:d4:5e:04:4f:d3:d6:e0:7b:37:97:cf:77:b0:de:ad:4a:47:ce:8c:96:49:5f:0a:cf:8c:ae:b2:6d:4b:8a:3f
		fly_drone-1.0.0.jar                               |Fly Drone                     |fly_drone                     |1.0.0               |DONE      |Manifest: NOSIGNATURE
		forge-1.20.1-47.4.20-universal.jar                |Forge                         |forge                         |47.4.20             |DONE      |Manifest: NOSIGNATURE
	Crash Report UUID: 56211aca-a6a1-472f-8380-a683ee6e4d06
	FML: 47.4
	Forge: net.minecraftforge:47.4.20
