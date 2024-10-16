package org.simbrain.network.learningrules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class ShortTermPlasticityTest {

    var net = Network()
    val n1 = Neuron()
    val n2 = Neuron()
    var s12 = Synapse(n1,n2)

    init {
        net.addNetworkModels(n1, n2, s12)
        s12.learningRule = ShortTermPlasticityRule().apply {
            plasticityType = 1
            firingThreshold = 0.0
            baseLineStrength = 1.0
            inputThreshold = 0.0
            bumpRate = .5
            decayRate = .2
        }

        s12.strength = 0.0
        n1.clamped = true
        n2.clamped = true
    }

    @Test
    fun `test basic update`() {
        n1.activation = 1.0
        n2.activation = 1.0
        net.update()
//        assertEquals(1.0,s12.strength )
//        net.update()
//        assertEquals(2.0,s12.strength )
        println("Strength is ${s12.strength}")
    }
}