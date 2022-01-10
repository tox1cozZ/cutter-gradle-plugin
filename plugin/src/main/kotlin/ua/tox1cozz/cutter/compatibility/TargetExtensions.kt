package ua.tox1cozz.cutter.compatibility

import ua.tox1cozz.cutter.configuration.TargetConfiguration
import ua.tox1cozz.cutter.configuration.TargetConfigurationContainer

//fun TargetConfigurationContainer.minecraftForgeSideOnlyLegacy() {
//    clientServerTarget(
//        "minecraftForgeSideOnlyLegacy",
//        "cpw/mods/fml/relauncher/SideOnly",
//        "cpw/mods/fml/relauncher/Side"
//    )
//}
//
//// TODO: Заполнить 1.12 аннотации
//fun TargetConfigurationContainer.minecraftForgeSideOnly() {
//    clientServerTarget(
//        "minecraftForgeSideOnly",
//        "cpw/mods/fml/relauncher/SideOnly",
//        "cpw/mods/fml/relauncher/Side"
//    )
//}
//
//fun TargetConfigurationContainer.minecraftForgeOnlyIn() {
//    clientServerTarget(
//        "minecraftForgeOnlyIn",
//        "net/minecraftforge/api/distmarker/OnlyIn",
//        "net/minecraftforge/api/distmarker/Dist",
//        serverValue = "DEDICATED_SERVER"
//    )
//}
//
//fun TargetConfigurationContainer.minecraftFabricEnvironment() {
//    clientServerTarget(
//        "minecraftFabricEnvironment",
//        "net/fabricmc/api/Environment",
//        "net/fabricmc/api/EnvType"
//    )
//}
//
//@JvmOverloads
//fun TargetConfigurationContainer.clientServerTarget(
//    name: String,
//    annotationType: String,
//    valueType: String,
//    parameterName: String? = null,
//    clientValue: String = "CLIENT",
//    serverValue: String = "SERVER",
//) {
//    named(TargetConfiguration.CLIENT_NAME) { client ->
//        client.types.register(name) { cutter ->
//            cutter.annotation.set(annotationType)
//            parameterName?.let { cutter.parameterName.set(it) }
//            cutter.value.set("$valueType.$clientValue")
//        }
//    }
//    named(TargetConfiguration.SERVER_NAME) { server ->
//        server.types.register(name) { cutter ->
//            cutter.annotation.set(annotationType)
//            parameterName?.let { cutter.parameterName.set(it) }
//            cutter.value.set("$valueType.$serverValue")
//        }
//    }
//}