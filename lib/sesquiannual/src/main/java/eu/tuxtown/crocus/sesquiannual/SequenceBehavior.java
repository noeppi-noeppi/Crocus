package eu.tuxtown.crocus.sesquiannual;

import biweekly.parameter.Range;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * Specifies, how sequences numbers should be treated in an {@link VEventGroup event group}. The behaviour required by
 * the iCalendar standard is {@link #OBSOLETE}. The other values exist because a lot of calendar software handles sequence
 * numbers in different ways leading to different interpretations, when events take place.
 *
 * @see VEventGroup#instances(SequenceBehavior)
 */
@NotNullByDefault
public enum SequenceBehavior {

    /**
     * Require all events in an event group to have the same sequence number. If that is not the case, throw an exception.
     */
    UNIFORM,

    /**
     * Higher sequence numbers obsolete lower sequence numbers. Sequence numbers are treated globally on the whole event
     * group. Therefore, if the sequence number of the main event is higher than the sequence number of a specific
     * repetition instance of that event, the repetition instance is ignored entirely.
     */
    OBSOLETE,

    /**
     * Higher sequence numbers obsolete lower sequence numbers. Sequence numbers are treated locally per repetition instance.
     * Therefore, if the sequence number of the main event is higher than the sequence number of a specific
     * repetition instance of that event, the repetition instance is still used. However, if there are two events that
     * mark the same repetition instance, the one with the lower sequence number is ignored.
     *
     * @apiNote This sequence behaviour does not support {@link Range#THIS_AND_FUTURE} repetition instances.
     */
    ISOLATE,

    /**
     * Sequence numbers are ignored entirely. Every event is used, even if multiple events mark the same repetition instance.
     * This renders sequence numbers almost entirely useless, however it seems to be actually used out there.
     *
     * @apiNote This sequence behaviour does not support {@link Range#THIS_AND_FUTURE} repetition instances.
     */
    COEXIST
}
