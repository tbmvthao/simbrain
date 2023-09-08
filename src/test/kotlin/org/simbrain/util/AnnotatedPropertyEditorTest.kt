package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.NumericWidget2
import org.simbrain.util.propertyeditor.StringWidget
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * Also see [AnnotatedPropertyEditorTestObject.java]
 */
class AnnotatedPropertyEditorTest {

    var net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)

    // Todo
    //  Check each data type
    //  Check each widget type (see Parameter Widget and org.simbrain.util.widgets)
    //  Test as many fields of UserParameter as possible. Esp min / max.
    //  Check internal list of todos

    @Test
    fun `test commit numeric widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        (ape.propertyWidgetMap[prop] as NumericWidget2).widget.value = .75
        ape.commitChanges()
        assertEquals(.75, n1.activation)
    }

    @Test
    fun `test fill field value numeric widget`() {
        n1.forceSetActivation(.75)
        val ape = AnnotatedPropertyEditor(n1)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        val widgetVal = (ape.propertyWidgetMap[prop] as NumericWidget2).widget.value
        assertEquals(.75, widgetVal)
    }

    @Test
    fun `test commit string widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        val prop = NetworkModel::class.declaredMemberProperties.first { it.name == "label" }
        (ape.propertyWidgetMap[prop as KProperty1<*, *>] as StringWidget).widget.text = "test"
        ape.commitChanges()
        assertEquals("test", n1.label)
    }

    @Test
    fun `test fill field value string widget`() {
        n1.label = "test"
        val ape = AnnotatedPropertyEditor(n1)
        val prop = NetworkModel::class.declaredMemberProperties.first { it.name == "label" }
        val widgetVal = (ape.propertyWidgetMap[prop as KProperty1<*, *>] as StringWidget).widget.text
        assertEquals("test", widgetVal)
    }

    @Test
    fun `test behavior two consistent values`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.75)
        val ape = AnnotatedPropertyEditor(n1, n2)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        assertEquals(true, (ape.propertyWidgetMap[prop] as NumericWidget2).isConsistent)
        (ape.propertyWidgetMap[prop] as NumericWidget2).widget.value = .25
        ape.commitChanges()
        assertEquals(.25, n1.activation)
        assertEquals(.25, n2.activation)
    }

    @Test
    fun `test behavior with inconsistent values`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.74)
        val ape = AnnotatedPropertyEditor(n1, n2)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        assertEquals(false, (ape.propertyWidgetMap[prop] as NumericWidget2).isConsistent)
        (ape.propertyWidgetMap[prop] as NumericWidget2).widget.value = .25
        ape.commitChanges()
        assertEquals(.25, n1.activation)
        assertEquals(.25, n2.activation)
    }

}