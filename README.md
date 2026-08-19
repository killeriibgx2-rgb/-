---- Minecraft Crash Report ----
// There are four lights!

Time: 2026-08-19 18:00:41
Description: Initializing game

java.lang.RuntimeException: {MINECRAFT_ACCESS_TOKEN}
	at net.minecraftforge.registries.GameData.postRegisterEvents(GameData.java:315) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.common.ForgeStatesProvider.lambda$new$4(ForgeStatesProvider.java:25) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.fml.ModLoader.handleInlineTransition(ModLoader.java:217) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.fml.ModLoader.lambda$dispatchAndHandleError$19(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at java.util.Optional.ifPresent(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.ModLoader.dispatchAndHandleError(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.fml.ModLoader.lambda$gatherAndInitializeMods$13(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at java.lang.Iterable.forEach(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.client.loading.ClientModLoader.lambda$begin$1(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.client.loading.ClientModLoader.lambda$createRunnableWithCatch$4(ClientModLoader.java:86) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.client.loading.ClientModLoader.begin(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraft.client.Minecraft.<init>(Minecraft.java:459) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
	at net.minecraft.client.main.Main.main(Main.java:182) ~[1.20.1-47.4.20.jar:?] {re:classloading}
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
	Suppressed: net.minecraftforge.fml.ModLoadingException: Fly Drone (fly_drone) encountered an error during the common_setup event phase
§7java.lang.RuntimeException: Unable to have damage AND stack.
		at net.minecraftforge.fml.javafmlmod.FMLModContainer.acceptEvent(FMLModContainer.java:125) ~[javafmllanguage-1.20.1-47.4.20.jar%23160!/:?] {}
		at net.minecraftforge.fml.ModLoader.lambda$postEventWithWrapInModOrder$33(ModLoader.java:346) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at java.lang.Iterable.forEach(Unknown Source) ~[?:?] {}
		at net.minecraftforge.fml.ModList.forEachModInOrder(ModList.java:227) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.fml.ModLoader.postEventWithWrapInModOrder(ModLoader.java:344) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.fml.ModLoader.postEventWrapContainerInModOrder(ModLoader.java:337) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.registries.GameData.postRegisterEvents(GameData.java:329) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.common.ForgeStatesProvider.lambda$new$4(ForgeStatesProvider.java:25) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.fml.ModLoader.handleInlineTransition(ModLoader.java:217) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.fml.ModLoader.lambda$dispatchAndHandleError$19(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at java.util.Optional.ifPresent(Unknown Source) ~[?:?] {}
		at net.minecraftforge.fml.ModLoader.dispatchAndHandleError(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.fml.ModLoader.lambda$gatherAndInitializeMods$13(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at java.lang.Iterable.forEach(Unknown Source) ~[?:?] {}
		at net.minecraftforge.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
		at net.minecraftforge.client.loading.ClientModLoader.lambda$begin$1(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.client.loading.ClientModLoader.lambda$createRunnableWithCatch$4(ClientModLoader.java:86) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.client.loading.ClientModLoader.begin(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraft.client.Minecraft.<init>(Minecraft.java:459) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
		at net.minecraft.client.main.Main.main(Main.java:182) ~[1.20.1-47.4.20.jar:?] {re:classloading}
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
	Caused by: java.lang.RuntimeException: Unable to have damage AND stack.
		at net.minecraft.world.item.Item$Properties.m_41487_(Item.java:434) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading}
		at com.zzynes.fly_drone.ModItems.lambda$static$1(ModItems.java:19) ~[fly_drone-1.0.0.jar%23157!/:1.0.0] {re:classloading}
		at net.minecraftforge.registries.DeferredRegister.lambda$addEntries$1(DeferredRegister.java:386) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.registries.RegisterEvent.register(RegisterEvent.java:59) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading,pl:eventbus:A}
		at net.minecraftforge.registries.DeferredRegister.addEntries(DeferredRegister.java:386) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.registries.DeferredRegister$EventDispatcher.handleEvent(DeferredRegister.java:328) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
		at net.minecraftforge.registries.__EventDispatcher_handleEvent_RegisterEvent.invoke(.dynamic) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading,pl:eventbus:B}
		at net.minecraftforge.eventbus.ASMEventHandler.invoke(ASMEventHandler.java:55) ~[eventbus-6.2.33.jar%23133!/:?] {}
		at net.minecraftforge.eventbus.EventBus.post(EventBus.java:312) ~[eventbus-6.2.33.jar%23133!/:?] {}
		at net.minecraftforge.eventbus.EventBus.post(EventBus.java:298) ~[eventbus-6.2.33.jar%23133!/:?] {}
		at net.minecraftforge.fml.javafmlmod.FMLModContainer.acceptEvent(FMLModContainer.java:121) ~[javafmllanguage-1.20.1-47.4.20.jar%23160!/:?] {}
		... 34 more


A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: Render thread
Suspected Mods: NONE
Stacktrace:
	at net.minecraftforge.registries.GameData.postRegisterEvents(GameData.java:315) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.common.ForgeStatesProvider.lambda$new$4(ForgeStatesProvider.java:25) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.fml.ModLoader.handleInlineTransition(ModLoader.java:217) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.fml.ModLoader.lambda$dispatchAndHandleError$19(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at java.util.Optional.ifPresent(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.ModLoader.dispatchAndHandleError(ModLoader.java:209) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.fml.ModLoader.lambda$gatherAndInitializeMods$13(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at java.lang.Iterable.forEach(Unknown Source) ~[?:?] {}
	at net.minecraftforge.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:183) ~[fmlcore-1.20.1-47.4.20.jar%23159!/:?] {}
	at net.minecraftforge.client.loading.ClientModLoader.lambda$begin$1(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.client.loading.ClientModLoader.lambda$createRunnableWithCatch$4(ClientModLoader.java:86) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraftforge.client.loading.ClientModLoader.begin(ClientModLoader.java:66) ~[forge-1.20.1-47.4.20-universal.jar%23163!/:?] {re:classloading}
	at net.minecraft.client.Minecraft.<init>(Minecraft.java:459) ~[client-1.20.1-20230612.114412-srg.jar%23158!/:?] {re:classloading,pl:accesstransformer:B}
-- Initialization --
Details:
	Modules: 
		ADVAPI32.dll:Расширенная библиотека API Windows 32:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		COMCTL32.dll:Библиотека элементов управления взаимодействия с пользователем:6.10 (WinBuild.160101.0800):Microsoft Corporation
		CRYPT32.dll:API32 криптографии:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		CRYPTBASE.dll:Base cryptographic API DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		CRYPTSP.dll:Cryptographic Service Provider API:10.0.19041.5856 (WinBuild.160101.0800):Microsoft Corporation
		ColorAdapterClient.dll:Microsoft Color Adapter Client:10.0.19041.5129 (WinBuild.160101.0800):Microsoft Corporation
		CoreMessaging.dll:Microsoft CoreMessaging Dll:10.0.19041.5915:Microsoft Corporation
		CoreUIComponents.dll:Microsoft Core UI Components Dll:10.0.19041.3636:Microsoft Corporation
		DBGHELP.DLL:Windows Image Helper:10.0.19041.5848 (WinBuild.160101.0800):Microsoft Corporation
		DEVOBJ.dll:Device Information Set DLL:10.0.19041.5794 (WinBuild.160101.0800):Microsoft Corporation
		DNSAPI.dll:Динамическая библиотека API DNS-клиента:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		GDI32.dll:GDI Client DLL:10.0.19041.6157 (WinBuild.160101.0800):Microsoft Corporation
		GLU32.dll:Библиотека подпрограмм OpenGL:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		IMM32.DLL:Multi-User Windows IMM32 API Client DLL:10.0.19041.6157 (WinBuild.160101.0800):Microsoft Corporation
		IPHLPAPI.DLL:API вспомогательного приложения IP:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		KERNEL32.DLL:Библиотека клиента Windows NT BASE API:10.0.19041.5915 (WinBuild.160101.0800):Microsoft Corporation
		KERNELBASE.dll:Библиотека клиента Windows NT BASE API:10.0.19041.5915 (WinBuild.160101.0800):Microsoft Corporation
		MSASN1.dll:ASN.1 Runtime APIs:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		MSCTF.dll:Серверная библиотека MSCTF:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		MpOav.dll:IOfficeAntiVirus Module:4.18.26070.9 (797b3603749206dfffe67a1cc891531b0a2cf438):Microsoft Corporation
		NLAapi.dll:Network Location Awareness 2:10.0.19041.6456 (WinBuild.160101.0800):Microsoft Corporation
		NSI.dll:NSI User-mode interface DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		NTASN1.dll:Microsoft ASN.1 API:10.0.19041.1 (WinBuild.160101.0800):Microsoft Corporation
		OLEAUT32.dll:OLEAUT32.DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		Ole32.dll:Microsoft OLE для Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		POWRPROF.dll:DLL модуля поддержки профиля управления питанием:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		PROPSYS.dll:Система страниц свойств (Майкрософт):7.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		PSAPI.DLL:Process Status Helper:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		Pdh.dll:Модуль поддержки данных производительности Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		RPCRT4.dll:Библиотека удаленного вызова процедур:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		SETUPAPI.dll:Windows Setup API:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		SHCORE.dll:SHCORE:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		SHELL32.dll:Общая библиотека оболочки Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		UMPDC.dll
		USER32.dll:Многопользовательская библиотека клиента USER API Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		USERENV.dll:Userenv:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		VCRUNTIME140.dll:Microsoft® C Runtime Library:14.40.33810.0:Microsoft Corporation
		VERSION.dll:Version Checking and File Installation Libraries:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		WINHTTP.dll:Службы HTTP Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		WINMM.dll:MCI API DLL:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		WINTRUST.dll:Microsoft Trust Verification APIs:10.0.19041.6456 (WinBuild.160101.0800):Microsoft Corporation
		WS2_32.dll:32-разрядная библиотек�� Windows Socket 2.0:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		Wldp.dll:Политика блокировки Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		amdihk64.dll:Radeon Settings: Host Service:2,00,00,1788:Advanced Micro Devices, Inc.
		amsi.dll:Anti-Malware Scan Interface:10.0.19041.4355 (WinBuild.160101.0800):Microsoft Corporation
		atig6pxx.dll:atiglpxx.dll:8.14.01.6564:Advanced Micro Devices, Inc. 
		atio6axx.dll:AMD OpenGL Driver:25.12.230729_569461f:Advanced Micro Devices, Inc
		bcrypt.dll:Библиотека криптографических примитивов Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		bcryptPrimitives.dll:Windows Cryptographic Primitives Library:10.0.19041.5007 (WinBuild.160101.0800):Microsoft Corporation
		cfgmgr32.dll:Configuration Manager DLL:10.0.19041.3996 (WinBuild.160101.0800):Microsoft Corporation
		clbcatq.dll:COM+ Configuration Catalog:2001.12.10941.16384 (WinBuild.160101.0800):Microsoft Corporation
		combase.dll:Microsoft COM для Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		dbgcore.DLL:Windows Core Debugging Helpers:10.0.19041.5794 (WinBuild.160101.0800):Microsoft Corporation
		dhcpcsvc.DLL:Служба DHCP-клиента:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		dhcpcsvc6.DLL:Клиент DHCPv6:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		dinput8.dll:Microsoft DirectInput:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		dwmapi.dll:Интерфейс API диспетчера окон рабочего стола (Майкрософт):10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		extnet.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		fwpuclnt.dll:API пользовательского режима FWP/IPsec:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		gdi32full.dll:GDI Client DLL:10.0.19041.6157 (WinBuild.160101.0800):Microsoft Corporation
		glfw.dll:GLFW 3.4.0 DLL:3.4.0:GLFW
		icm32.dll:Microsoft Color Management Module (CMM):10.0.19041.5129 (WinBuild.160101.0800):Microsoft Corporation
		iertutil.dll:Служебная программа времени выполнения для Internet Explorer:11.00.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		inputhost.dll:InputHost:10.0.19041.5794 (WinBuild.160101.0800):Microsoft Corporation
		java.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		javaw.exe:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		jemalloc.dll
		jimage.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		jli.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		jna13999454230040970523.dll:JNA native library:6.1.4:Java(TM) Native Access (JNA)
		jsvml.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		jvm.dll:Zulu 64-Bit Server VM:17.0.18:Azul Systems Inc.
		kernel.appcore.dll:AppModel API Host:10.0.19041.3758 (WinBuild.160101.0800):Microsoft Corporation
		lwjgl.dll
		lwjgl_opengl.dll
		lwjgl_stb.dll
		management.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		management_ext.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		mdnsNSP.dll:Bonjour Namespace Provider:3,1,0,1:Apple Inc.
		mscms.dll:DLL-библиотека системы сопоставления цветов Майкрософт:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		msvcp140.dll:Microsoft® C Runtime Library:14.40.33810.0:Microsoft Corporation
		msvcp_win.dll:Microsoft® C Runtime Library:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		msvcrt.dll:Windows NT CRT DLL:7.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		mswsock.dll:Расширение поставщика службы API Microsoft Windows Sockets 2.0:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		napinsp.dll:Поставщик оболочки совместимости для имен электронной почты:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		ncrypt.dll:Маршрутизатор Windows NCrypt:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		net.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		netutils.dll:Net Win32 API Helpers DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		nio.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		ntdll.dll:Системная библиотека NT:10.0.19041.4842 (WinBuild.160101.0800):Microsoft Corporation
		ntmarta.dll:Поставщик Windows NT MARTA:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		opengl32.dll:OpenGL Client DLL:10.0.19041.5794 (WinBuild.160101.0800):Microsoft Corporation
		pnrpnsp.dll:Поставщик пространства имен PNRP:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		profapi.dll:User Profile Basic API:10.0.19041.6033 (WinBuild.160101.0800):Microsoft Corporation
		rasadhlp.dll:Remote Access AutoDial Helper:10.0.19041.6328 (WinBuild.160101.0800):Microsoft Corporation
		rsaenh.dll:Microsoft Enhanced Cryptographic Provider:10.0.19041.1 (WinBuild.160101.0800):Microsoft Corporation
		sechost.dll:Host for SCM/SDDL/LSA Lookup APIs:10.0.19041.1 (WinBuild.160101.0800):Microsoft Corporation
		shlwapi.dll:Библиотека небольших программ оболочки:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		srvcli.dll:Server Service Client DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		sunmscapi.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		textinputframework.dll:"TextInputFramework.DYNLINK":10.0.19041.6456 (WinBuild.160101.0800):Microsoft Corporation
		ucrtbase.dll:Microsoft® C Runtime Library:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		urlmon.dll:Расширения OLE32 для Win32:11.00.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		uxtheme.dll:Библиотека тем UxTheme (Microsoft):10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		vcruntime140_1.dll:Microsoft® C Runtime Library:14.40.33810.0:Microsoft Corporation
		verify.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
		win32u.dll:Win32u:10.0.19041.6456 (WinBuild.160101.0800):Microsoft Corporation
		windows.storage.dll:API хранения Microsoft WinRT:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		winrnr.dll:LDAP RnR Provider DLL:10.0.19041.3636 (WinBuild.160101.0800):Microsoft Corporation
		wintypes.dll:Библиотека DLL основных типов Windows:10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		wshbth.dll:Windows Sockets Helper DLL:10.0.19041.5848 (WinBuild.160101.0800):Microsoft Corporation
		xinput1_4.dll:API общего контроллера (Майкрософт):10.0.19041.4522 (WinBuild.160101.0800):Microsoft Corporation
		zip.dll:Zulu Platform x64 Architecture:17.0.18:Azul Systems Inc.
Stacktrace:
	at net.minecraft.client.main.Main.main(Main.java:182) ~[1.20.1-47.4.20.jar:?] {re:classloading}
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
	Memory: 112711160 bytes (107 MiB) / 352321536 bytes (336 MiB) up to 8388608000 bytes (8000 MiB)
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
	Virtual memory used (MB): 12135.99
	Swap memory total (MB): 4864.00
	Swap memory used (MB): 0.00
	JVM Flags: 2 total; -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump -Xmx8000M
	Launched Version: 1.20.1
	Backend library: LWJGL version 3.3.1 build 7
	Backend API: AMD Radeon RX 580 2048SP GL version 4.6.0 Core Profile Context 26.1.1.251211, ATI Technologies Inc.
	Window size: <not initialized>
	GL Caps: Using framebuffer using OpenGL 3.2
	GL debug messages: 
	Using VBOs: Yes
	Is Modded: Definitely; Client brand changed to 'forge'
	Type: Client (map_client.txt)
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
		client-1.20.1-20230612.114412-srg.jar             |Minecraft                     |minecraft                     |1.20.1              |COMMON_SET|Manifest: a1:d4:5e:04:4f:d3:d6:e0:7b:37:97:cf:77:b0:de:ad:4a:47:ce:8c:96:49:5f:0a:cf:8c:ae:b2:6d:4b:8a:3f
		fly_drone-1.0.0.jar                               |Fly Drone                     |fly_drone                     |1.0.0               |COMMON_SET|Manifest: NOSIGNATURE
		forge-1.20.1-47.4.20-universal.jar                |Forge                         |forge                         |47.4.20             |COMMON_SET|Manifest: NOSIGNATURE
	Crash Report UUID: 3ed0e272-862e-4458-b2ee-dff8704da68e
	FML: 47.4
	Forge: net.minecraftforge:47.4.20
