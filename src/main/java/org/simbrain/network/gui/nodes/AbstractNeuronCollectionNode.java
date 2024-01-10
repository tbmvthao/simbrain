package org.simbrain.network.gui.nodes;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.events.NeuronCollectionEvents;
import org.simbrain.network.events.NeuronEvents;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.piccolo.Outline;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.simbrain.util.SwingUtilsKt.getSwingDispatcher;

public abstract class AbstractNeuronCollectionNode extends ScreenElement {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * The outlined objects (neurons) for this neuron group.
     */
    private final Outline outlinedObjects;

    /**
     * The interaction box for this neuron collection
     */
    private InteractionBox interactionBox;

    /**
     * Constituent neuron nodes.
     */
    private Set<NeuronNode> neuronNodes = new HashSet<>();

    /**
     * Reference to neuron group or collection.
     */
    private AbstractNeuronCollection nc;

    public AbstractNeuronCollectionNode(NetworkPanel networkPanel, AbstractNeuronCollection group) {
        super(networkPanel);
        this.networkPanel = networkPanel;
        this.nc = group;
        outlinedObjects = new Outline();

        addChild(outlinedObjects);

        NeuronCollectionEvents events = nc.getEvents();
        events.getDeleted().on(getSwingDispatcher(), n ->  {
            removeFromParent();
        });
        events.getLabelChanged().on(getSwingDispatcher(), (o,n) -> {
            updateText();
        });
        events.getLocationChanged().on(getSwingDispatcher(), () -> {
            pullPositionFromModel();
            outlinedObjects.updateBounds();
        });

        events.getRecordingStarted().on(this::updateText);
        events.getRecordingStopped().on(this::updateText);

    }

    /**
     * Select the neurons in this group.
     */
    public void selectNeurons() {
        getNeuronNodes().stream().map(NeuronNode::getNeuron).forEach(NetworkModel::select);
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (this.getVisible()) {
            interactionBox.setOffset(outlinedObjects.getFullBounds().getX() + Outline.ARC_SIZE / 2,
                    outlinedObjects.getFullBounds().getY() - interactionBox.getFullBounds().getHeight() + 1);
        }
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void pullPositionFromModel() {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.pullViewPositionFromModel();
        }
        outlinedObjects.resetOutlinedNodes(neuronNodes);
    }

    @Override
    public void offset(double dx, double dy) {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.offset(dx, dy);
        }
    }

    /**
     * Set what neuron nodes will be outlined.
     */
    public void addNeuronNodes(Collection<NeuronNode> neuronNodes) {
        this.neuronNodes.addAll(neuronNodes);
        for (NeuronNode neuronNode : neuronNodes) {
            // Listen directly to neuron nodes for property change events
            NeuronEvents neuronEvents = neuronNode.getNeuron().getEvents();
            neuronEvents.getDeleted().on(getSwingDispatcher(), n -> {
                this.neuronNodes.remove(neuronNode);
                outlinedObjects.resetOutlinedNodes(this.neuronNodes);
            });
            neuronEvents.getLocationChanged().on(getSwingDispatcher(), () -> outlinedObjects.resetOutlinedNodes(this.neuronNodes));
            neuronEvents.getLabelChanged().on(getSwingDispatcher(), (o,n) -> outlinedObjects.resetOutlinedNodes(this.neuronNodes));
        }
        outlinedObjects.resetOutlinedNodes(this.neuronNodes);
    }

    public void removeNeuronNode(NeuronNode neuronNode) {
        neuronNodes.remove(neuronNode);
    }

    public abstract AbstractNeuronCollection getModel();

    public InteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * Set a custom interaction box.  Subclasses can call this to customize its behavior.
     *
     * @param newBox the newBox to set.
     */
    protected void setInteractionBox(InteractionBox newBox) {
        this.removeChild(interactionBox);
        this.interactionBox = newBox;
        this.addChild(interactionBox);
    }

    /**
     * Default text update. Override for more specific behavior.
     */
    public void updateText() {
        // Set text to label by default
        String text = nc.getLabel();

        // If there is state info, use that instead of a label
        if (!nc.getStateInfo().isEmpty()) {
            text = nc.getStateInfo();
        }

        // Update the text
        getInteractionBox().setText(text);
    }

    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Neuron Collection...") {
        @Override
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", getModel().getLabel());
            getModel().setLabel(newName);
        }
    };

    public Set<NeuronNode> getNeuronNodes() {
        return neuronNodes;
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

}
