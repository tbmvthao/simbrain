package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.toDoubleArray
import smile.math.matrix.Matrix
import kotlin.math.exp

class SoftmaxRule: NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), BoundedUpdateRule {

    @UserParameter(
        label = "Temperature",
        description = """Above 1 is a "hotter", more chaotic, and thus flatter distribution. 0 to 1 is a "cooler", more predictable, sharper distribution.""",
        minimumValue = 0.0,
        increment = .1,
        order = 10)
    var temperature = 1.0

    private fun softmax(input: Matrix, temperature: Double, bias: Matrix = Matrix(input.nrow(), 1)): DoubleArray {
        // These are often called "logits", that is, a set of unnormalized values
        val exponentials = (input.toDoubleArray() zip bias.toDoubleArray()).map { (i, b) -> exp((i + b)/temperature) }
        val total = exponentials.sum()
        return exponentials.map { it/total }.toDoubleArray()
    }

    context(Network) override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        layer.setActivations(softmax(layer.inputs, temperature, layer.biases))
    }

    context(Network) override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("SoftmaxRule does not support scalar data")
    }

    override val name = "Softmax"
    override val timeType = Network.TimeType.DISCRETE

    override fun createMatrixData(size: Int): EmptyMatrixData {
        return EmptyMatrixData
    }

    override fun copy() = SoftmaxRule().also {
        it.temperature = temperature
    }

    override var upperBound: Double
        get() = 1.0
        set(value) {}

    override var lowerBound: Double
        get() = 0.0
        set(value) {}
}