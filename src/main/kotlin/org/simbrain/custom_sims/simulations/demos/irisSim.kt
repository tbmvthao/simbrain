package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toMatrix
import smile.io.Read

/**
 * Train a smile classifier on Iris data.
 */
val irisClassifier = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Last column is target data
    val iris = Read.arff("simulations/tables/iris.arff")

    // Add a neuron collection for setting inputs to the network
    val inputNc = network.addNeuronCollection(4).apply{
        label = "Inputs"
        setClamped(true)
        location = point(0, 0)
    }

    // Choose a classifier here
    // val classifier = LogisticRegClassifier(4, 3)
    // val classifier = SVMClassifier(4, 3)
    val classifier = KNNClassifier(4, 3)

    classifier.trainingData.featureVectors = iris.select(0,1,2,3).toArray()
    classifier.trainingData.targetLabels = iris.column(4).toStringArray()
    val smileClassifier = SmileClassifier(classifier)
    smileClassifier.train()

    // Set input data for iris to training data
    inputNc.inputData = classifier.trainingData.featureVectors.toMatrix()

    val weightMatrix = WeightMatrix(inputNc, smileClassifier)
    network.addNetworkModels(weightMatrix, smileClassifier)
    smileClassifier.location = point(0, -300)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 500
        }
    }

}