package ua.tox1cozz.cutter

import kotlin.math.pow

@CutterTargetOnly(CutterTarget.SERVER)
class CutterTest(private val capture: Boolean) {

    val local = "Hello"

    val number: Int = Cutter.fieldValue(CutterTarget.SERVER) {
        Cutter.fieldValue(CutterTarget.CLIENT) {
            local.hashCode() + 1488 + capture.hashCode()
        }
    }

    val numberServer: Int = Cutter.fieldValue(CutterTarget.SERVER) {
        2.0.pow(4.0).toInt()
    }

    @CutterTargetOnly(CutterTarget.CLIENT)
    fun testClient() {
        println("CLIENT METHOD")
    }

    @CutterTargetOnly(CutterTarget.SERVER)
    fun testServer() {
        println("SERVER METHOD")
    }

    @CutterTargetOnly(CutterTarget.DEBUG)
    fun testDebug() {
        println("DEBUG METHOD")
    }

    inner class Inner {

        init {
            Cutter.execute(CutterTarget.SERVER) {
                println("Hello Inner!")
            }
        }
    }

    class Vlojenniy {

        init {
            Cutter.execute(CutterTarget.SERVER) {
                println("Hello Vlojenniy!")
            }
        }
    }
}