---- Minecraft Crash Report ----
// Shall we play a game?

Time: 2026-08-19 18:08:23
Description: mouseClicked event handler

java.lang.IllegalStateException: Failed to load registries due to above errors
	at net.minecraft.resources.RegistryDataLoader.m_247207_(RegistryDataLoader.java:77) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_246152_(WorldLoader.java:54) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_245736_(WorldLoader.java:58) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_214362_(WorldLoader.java:31) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.m_232896_(CreateWorldScreen.java:125) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.worldselection.SelectWorldScreen.m_279861_(SelectWorldScreen.java:60) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.Button.m_5691_(Button.java:38) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.AbstractButton.m_5716_(AbstractButton.java:55) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.AbstractWidget.m_6375_(AbstractWidget.java:175) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.events.ContainerEventHandler.m_6375_(ContainerEventHandler.java:38) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.MouseHandler.m_168084_(MouseHandler.java:92) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.Screen.m_96579_(Screen.java:437) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91530_(MouseHandler.java:89) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.MouseHandler.m_168091_(MouseHandler.java:189) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.execute(BlockableEventLoop.java:102) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91565_(MouseHandler.java:188) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at org.lwjgl.glfw.GLFWMouseButtonCallbackI.callback(GLFWMouseButtonCallbackI.java:43) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
	at org.lwjgl.system.JNI.invokeV(Native Method) ~[lwjgl-3.3.1.jar%23130!/:build 7] {}
	at org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout(GLFW.java:3474) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
	at com.mojang.blaze3d.systems.RenderSystem.limitDisplayFPS(RenderSystem.java:237) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.Minecraft.m_91383_(Minecraft.java:1173) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
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


A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Render thread
Suspected Mods: NONE
Stacktrace:
	at net.minecraft.resources.RegistryDataLoader.m_247207_(RegistryDataLoader.java:77) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_246152_(WorldLoader.java:54) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_245736_(WorldLoader.java:58) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.server.WorldLoader.m_214362_(WorldLoader.java:31) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.m_232896_(CreateWorldScreen.java:125) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.worldselection.SelectWorldScreen.m_279861_(SelectWorldScreen.java:60) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.Button.m_5691_(Button.java:38) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.AbstractButton.m_5716_(AbstractButton.java:55) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.AbstractWidget.m_6375_(AbstractWidget.java:175) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.components.events.ContainerEventHandler.m_6375_(ContainerEventHandler.java:38) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.MouseHandler.m_168084_(MouseHandler.java:92) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.gui.screens.Screen.m_96579_(Screen.java:437) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91530_(MouseHandler.java:89) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.MouseHandler.m_168091_(MouseHandler.java:189) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.execute(BlockableEventLoop.java:102) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91565_(MouseHandler.java:188) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at org.lwjgl.glfw.GLFWMouseButtonCallbackI.callback(GLFWMouseButtonCallbackI.java:43) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
	at org.lwjgl.system.JNI.invokeV(Native Method) ~[lwjgl-3.3.1.jar%23130!/:build 7] {}
	at org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout(GLFW.java:3474) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
-- Affected screen --
Details:
	Screen name: net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
Stacktrace:
	at net.minecraft.client.gui.screens.Screen.m_96579_(Screen.java:437) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91530_(MouseHandler.java:89) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.MouseHandler.m_168091_(MouseHandler.java:189) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.util.thread.BlockableEventLoop.execute(BlockableEventLoop.java:102) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.MouseHandler.m_91565_(MouseHandler.java:188) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at org.lwjgl.glfw.GLFWMouseButtonCallbackI.callback(GLFWMouseButtonCallbackI.java:43) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
	at org.lwjgl.system.JNI.invokeV(Native Method) ~[lwjgl-3.3.1.jar%23130!/:build 7] {}
	at org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout(GLFW.java:3474) ~[lwjgl-glfw-3.3.1.jar%2388!/:build 7] {}
	at com.mojang.blaze3d.systems.RenderSystem.limitDisplayFPS(RenderSystem.java:237) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.Minecraft.m_91383_(Minecraft.java:1173) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
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
	Finished: Yes
	Packs: vanilla, mod_resources
Stacktrace:
	at net.minecraft.client.ResourceLoadStateTracker.m_168562_(ResourceLoadStateTracker.java:49) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
	at net.minecraft.client.Minecraft.m_91354_(Minecraft.java:2326) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.Minecraft.m_91374_(Minecraft.java:735) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
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


-- System Details --
Details:
	Minecraft Version: 1.20.1
	Minecraft Version ID: 1.20.1
	Operating System: Windows 10 (amd64) version 10.0
	Java Version: 17.0.18, Azul Systems, Inc.
	Java VM Version: OpenJDK 64-Bit Server VM (mixed mode, sharing), Azul Systems, Inc.
	Memory: 224023344 bytes (213 MiB) / 620756992 bytes (592 MiB) up to 8388608000 bytes (8000 MiB)
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
	Virtual memory used (MB): 12774.75
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
	Crash Report UUID: 7312cbc6-665a-4e87-8831-11cb5bb4d329
	FML: 47.4
	Forge: net.minecraftforge:47.4.20
