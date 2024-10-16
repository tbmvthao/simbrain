package org.simbrain.world.odorworld.events

import org.simbrain.util.Events
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Sensor


interface EntityLocationEvent {
    val moved: Events.NoArgEvent
}

/**
 * See [Events].
 */
class EntityEvents: Events(), EntityLocationEvent {
    val updated = NoArgEvent()
    val typeChanged = ChangedEvent<EntityType>()
    val deleted = OneArgEvent<OdorWorldEntity>()
    val sensorAdded = OneArgEvent<Sensor>()
    val sensorRemoved = OneArgEvent<Sensor>()
    val effectorAdded = OneArgEvent<Effector>()
    val effectorRemoved = OneArgEvent<Effector>()
    val propertyChanged = NoArgEvent()
    val collided = OneArgEvent<Bounded>()
    override val moved = NoArgEvent()
    val trailVisibilityChanged = ChangedEvent<Boolean>()
    val trailCleared = NoArgEvent()

}
/**
 * See [Events]
 */
class SensorEffectorEvents: Events() {
    val updated = NoArgEvent()
    val propertyChanged = NoArgEvent()
}
