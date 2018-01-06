/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.workspace;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.workspace.gui.ComponentPanel;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Represents a component in a Simbrain {@link org.simbrain.workspace.Workspace}.
 * Extend this class to create your own component type. Gui representations of
 * a workspace component should extend {@link org.simbrain.workspace.gui.GuiComponent}.
 */
public abstract class WorkspaceComponent {

    /** The workspace that 'owns' this component. */
    private Workspace workspace;

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** The set of all WorkspaceComponentListeners on this component. */
    private Collection<WorkspaceComponentListener> workspaceComponentListeners;

    /** List of attribute listeners. */
    private Collection<AttributeListener> attributeListeners;

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

    /** List of producer types. */
    private List<AttributeType> producerTypes = new ArrayList<AttributeType>();

    /** List of consumer types. */
    private List<AttributeType> consumerTypes = new ArrayList<AttributeType>();

    /**
     * Whether to display the GUI for this component (obviously only relevant
     * when Simbrain is run as a GUI). TODO: This should really be a property of
     * the GUI only, since we can imagine the gui is on or off for different
     * views of the component. This design is kind of hack, based on the fact
     * that {@link ComponentPanel} has no easy access to {@link GuiComponent}.
     */
    private boolean guiOn = true;

    /** Whether to update this component. */
    private boolean updateOn = true;

    /** The name of this component. Used in the title, in saving, etc. */
    private String name = "";

    /**
     * Current file. Used when "saving" a component. Subclasses can provide a
     * default value using User Preferences.
     */
    private File currentFile;

    /**
     * If set to true, serialize this component before others. Possibly replace
     * with priority system later.
     * {@see org.simbrain.workspace.Workspace#preSerializationInit()}.
     */
    private int serializePriority = 0;

    /**
     * Initializer
     */
    {
        workspaceComponentListeners = new HashSet<WorkspaceComponentListener>();
        attributeListeners = new HashSet<AttributeListener>();
    }

    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(getClass().getCanonicalName() + ": " + name + " created");
    }

    /**
     * Used when saving a workspace. All changed workspace components are saved
     * using this method.
     *
     * @param output the stream of data to write the data to.
     * @param format a key used to define the requested format.
     */
    public abstract void save(OutputStream output, String format);

    /**
     * Returns a list of the formats that this component supports.
     * The default behavior is to return a list containing the default format.
     *
     * @return a list of the formats that this component supports.
     */
    public List<? extends String> getFormats() {
        return Collections.singletonList(getDefaultFormat());
    }
    
    /**
     * Fires an event which leads any linked gui components to close, 
     * which calls the haschanged dialog.
     */
    public void tryClosing() {
        fireComponentClosing();
        //TODO: If there is no Gui then close must be called directly
    }

    /**
     * Closes the WorkspaceComponent.
     */
    public void close() {
        closing();
        workspace.removeWorkspaceComponent(this);
    }

    /**
     * Perform cleanup after closing.
     */
    protected abstract void closing();

    /**
     * Called by Workspace to update the state of the component.
     */
    public void update() {
        /* no default implementation */
    }

    /**
     * Fire attribute object removed event (when the base object of an attribute
     * is removed).
     *
     * @param object the object which was removed
     */
    public void fireAttributeObjectRemoved(Object object) {
        for (AttributeListener listener : attributeListeners) {
            listener.attributeObjectRemoved(object);
        }
    }

    /**
     * Fire potential attributes changed event.
     */
    public void firePotentialAttributesChanged() {
        for (AttributeListener listener : attributeListeners) {
            listener.potentialAttributesChanged();
        }
    }

    /**
     * Fire attribute type visibility changed event.
     *
     * @param type the type whose visibility changed.
     */
    public void fireAttributeTypeVisibilityChanged(AttributeType type) {
        for (AttributeListener listener : attributeListeners) {
            listener.attributeTypeVisibilityChanged(type);
        }
    }

    /**
     * Adds a AttributeListener to this component.
     *
     * @param listener the AttributeListener to add.
     */
    public void addAttributeListener(final AttributeListener listener) {
        attributeListeners.add(listener);
    }

    /**
     * Removes an AttributeListener from this component.
     *
     * @param listener the AttributeListener to remove.
     */
    public void removeAttributeListener(AttributeListener listener) {
        attributeListeners.remove(listener);
    }

    /**
     * Add a new type of producer.
     *
     * @param type type to add
     */
    public void addProducerType(AttributeType type) {
        if (!producerTypes.contains(type)) {
            producerTypes.add(type);
        }
    }

    /**
     * Add a new type of consumer.
     *
     * @param type type to add
     */
    public void addConsumerType(AttributeType type) {
        if (!consumerTypes.contains(type)) {
            consumerTypes.add(type);
        }
    }

    /**
     * Finds objects based on a key. Used in deserializing attributes. Any class
     * that produces attributes should override this for serialization.
     *
     * @param objectKey String key
     * @return the corresponding object
     */
    public Object getObjectFromKey(final String objectKey) {
        return null;
    }

    /**
     * Returns a unique key associated with an object. Used in serializing
     * attributes. Any class that produces attributes should override this for
     * serialization.
     *
     * @param object object which should be associated with a key
     * @return the key
     */
    public String getKeyFromObject(Object object) {
        return null;
    }

    /**
     * Returns the locks for the update parts. There should be one lock per
     * part. These locks need to be the same ones used to lock the update of
     * each part.
     *
     * @return The locks for the update parts.
     */
    public Collection<? extends Object> getLocks() {
        return Collections.singleton(this);
    }

    /**
     * Called by Workspace to notify that updates have stopped.
     */
    protected void stopped() {
        /* no default implementation */
    }

    /**
     * Notify all workspaceComponentListeners of a componentUpdated event.
     */
    public final void fireUpdateEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.componentUpdated();
        }
    }

    /**
     * Notify all workspaceComponentListeners that the gui has been turned on or
     * off.
     */
    public final void fireGuiToggleEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.guiToggled();
        }
    }

    /**
     * Notify all workspaceComponentListeners of a component has been turned on
     * or off.
     */
    public final void fireComponentToggleEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.componentOnOffToggled();
        }
    }

    /**
     * Fired when component is closed.
     */
    public void fireComponentClosing() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.componentClosing();
        }
    }

    /**
     * Called after a global update ends.
     */
    final void doStopped() {
        stopped();
    }

    /**
     * Returns the WorkspaceComponentListeners on this component.
     *
     * @return The WorkspaceComponentListeners on this component.
     */
    public Collection<WorkspaceComponentListener> getWorkspaceComponentListeners() {
        return Collections.unmodifiableCollection(workspaceComponentListeners);
    }

    /**
     * Adds a WorkspaceComponentListener to this component.
     *
     * @param listener the WorkspaceComponentListener to add.
     */
    public void addWorkspaceComponentListener(
            final WorkspaceComponentListener listener) {
        workspaceComponentListeners.add(listener);
    }

    /**
     * Adds a WorkspaceComponentListener to this component.
     *
     * @param listener the WorkspaceComponentListener to add.
     */
    public void removeWorkspaceComponentListener(
            final WorkspaceComponentListener listener) {
        workspaceComponentListeners.remove(listener);
    }

    /**
     * Returns the name of this component.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
        // TODO: Think about this
        // for (WorkspaceComponentListener listener : this.getListeners()) {
        // listener.setTitle(name);
        // }
    }

    /**
     * Retrieves a simple version of a component name from its class, e.g.
     * "Network" from "org.simbrain.network.NetworkComponent"/
     *
     * @return the simple name.
     */
    public String getSimpleName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Component")) {
            simpleName = simpleName.replaceFirst("Component", "");
        }
        return simpleName;
    }

    /**
     * Override for use with open service.
     *
     * @return xml string representing stored file.
     */
    public String getXML() {
        return null;
    }

    /**
     * Sets the workspace for this component. Called by the workspace right
     * after this component is created.
     *
     * @param workspace The workspace for this component.
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns the workspace associated with this component.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * The file extension for a component type, e.g. By default, "xml".
     *
     * @return the file extension
     */
    public String getDefaultFormat() {
        return "xml";
    }

    /**
     * Set to true when a component changes, set to false after a component is
     * saved.
     *
     * @param changedSinceLastSave whether this component has changed since the
     *            last save.
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        logger.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * Returns true if it's changed since the last save.
     *
     * @return the changedSinceLastSave
     */
    public boolean hasChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * @return the currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile the currentFile to set
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    /**
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @return the guiOn
     */
    public boolean isGuiOn() {
        return guiOn;
    }

    /**
     * @param guiOn the guiOn to set
     */
    public void setGuiOn(boolean guiOn) {
        this.guiOn = guiOn;
        this.fireGuiToggleEvent();
    }

    /**
     * @return the updateOn
     */
    public boolean getUpdateOn() {
        return updateOn;
    }

    /**
     * @param updateOn the updateOn to set
     */
    public void setUpdateOn(boolean updateOn) {
        this.updateOn = updateOn;
        this.fireComponentToggleEvent();
    }

    /**
     * @return the producerTypes
     */
    public List<AttributeType> getProducerTypes() {
        return Collections.unmodifiableList(producerTypes);
    }

    /**
     * @return the consumerTypes
     */
    public List<AttributeType> getConsumerTypes() {
        return Collections.unmodifiableList(consumerTypes);
    }

    /**
     * Return visible producer types.
     *
     * @return the visible producerTypes
     */
    public List<AttributeType> getVisibleProducerTypes() {
        List<AttributeType> returnList = new ArrayList<AttributeType>();
        for (AttributeType type : getProducerTypes()) {
            if (type.isVisible()) {
                returnList.add(type);
            }
        }
        return returnList;
    }

    /**
     * Return visible consumer types.
     *
     * @return the visible consumerTypes
     */
    public List<AttributeType> getVisibleConsumerTypes() {
        List<AttributeType> returnList = new ArrayList<AttributeType>();
        for (AttributeType type : getConsumerTypes()) {
            if (type.isVisible()) {
                returnList.add(type);
            }
        }
        return returnList;
    }

    /**
     * @return the serializePriority
     */
    protected int getSerializePriority() {
        return serializePriority;
    }

    /**
     * @param serializePriority the serializePriority to set
     */
    protected void setSerializePriority(int serializePriority) {
        this.serializePriority = serializePriority;
    }

    /**
     * Called when a simulation begins, e.g. when the "run" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    public void start() {
    }

    /**
     * Called when a simulation stops, e.g. when the "stop" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    public void stop() {
    }

    public List<Producer2<?>> getProducers() {
        return getProducers(this);
    }

    public List<Consumer2<?>> getConsumers() {
        return getConsumers(this);
    }

    public List<Producer2<?>> getProducersFromList(List list) {
        List<Producer2<?>> returnList = new ArrayList<>();
        for (Object object : list) {
            returnList.addAll(getProducers(object));
        }
        return returnList;
    }

    public List<Consumer2<?>> getConsumersFromList(List list) {
        List<Consumer2<?>> returnList = new ArrayList<>();
        for (Object object : list) {
            returnList.addAll(getConsumers(object));
        }
        return returnList;
    }

    // TODO: Rename to getProducersOnObject... to clarify it's a service / helper
    public List<Producer2<?>> getProducers(Object object) {
        List<Producer2<?>> returnList = new ArrayList<Producer2<?>>();
        for (Method method : object.getClass().getMethods()) {
            Producible annotation = method.getAnnotation(Producible.class);
            if (annotation != null) {
                // A custom keyed annotation is being used
                if (!annotation.indexListMethod().isEmpty()) {
                    try {
                        Method indexListMethod = object.getClass()
                                .getMethod(annotation.indexListMethod(), null);
                        List keys = (List) indexListMethod.invoke(object, null);
                        for (Object key: keys) {
                            Producer2<?> consumer = new Producer2(this, object, method);
                            consumer.key = key;
                            returnList.add(consumer);
                        }
                    } catch (Exception e) {
                        // TODO: Use multicatch
                        e.printStackTrace();
                    }
                }  else {
                    // Annotation has no key
                    Producer2<?> producer = new Producer2(this, object, method);
                    //setCustomDescription(producer);
                    returnList.add(producer);
                }
            }
        }
        return returnList;
    }

    public List<Consumer2<?>> getConsumers(Object object) {
        List<Consumer2<?>> returnList = new ArrayList<>();
        for (Method method : object.getClass().getMethods()) {
            // TODO: Docs; When this works do it for producers
            // Key case
            Consumable annotation = method.getAnnotation(Consumable.class);
            if (annotation != null) {
                // A custom keyed annotation is being used
                if (!annotation.indexListMethod().isEmpty()) {
                    try {
                        Method indexListMethod = object.getClass()
                                .getMethod(annotation.indexListMethod(), null);
                        List keys = (List) indexListMethod.invoke(object, null);
                        for (Object key: keys) {
                            Consumer2<?> consumer = new Consumer2(this, object, method);
                            consumer.key = key;
                            returnList.add(consumer);
                        }
                    } catch (Exception e) {
                        // TODO: Use multicatch
                        e.printStackTrace();
                    }
                } else {
                    // Annotation has no key
                    Consumer2<?> consumer = new Consumer2(this, object, method);
                    //setCustomDescription(consumer);
                    returnList.add(consumer);
                }
            }
        }
        return returnList;
    }

    public Consumer2<?> getConsumer(Object object, String methodName) {
        return getConsumers(object).stream().filter(
                c -> c.getMethod().getName().equalsIgnoreCase(methodName))
                     .findFirst().get();
    }

    @SuppressWarnings("unchecked")
    public <T> Consumer2<T> getConsumer(Object object, String methodName, Class<T> type)
            throws MismatchedAttributesException {
        Consumer2<?> consumer = getConsumer(object, methodName);
        if (consumer.getType() == type) {
            return (Consumer2<T>) consumer;
        } else {
            throw new MismatchedAttributesException("Consumer type does not match method value type.");
        }
    }

    public Producer2<?> getProducer(Object object, String methodName) {
        return getProducers(object).stream().filter(
                p -> p.getMethod().getName().equalsIgnoreCase(methodName))
                     .findFirst().get();
    }

    @SuppressWarnings("unchecked")
    public <T> Producer2<T> getProducer(Object object, String methodName, Class<T> type)
            throws MismatchedAttributesException {
        Producer2<?> producer = getProducer(object, methodName);
        if (producer.getType() == type) {
            return (Producer2<T>) producer;
        } else {
            throw new MismatchedAttributesException("Producer type does not match method return type.");
        }
    }
}
