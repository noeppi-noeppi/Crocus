package eu.tuxtown.crocus.sesquiannual;

import biweekly.component.VEvent;
import biweekly.parameter.Range;
import biweekly.property.*;
import biweekly.util.Duration;
import biweekly.util.ICalDate;
import eu.tuxtown.crocus.sesquiannual.internal.StreamHelper;
import eu.tuxtown.crocus.sesquiannual.internal.TimeHelper;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * An {@link VEventGroup event group} is a collection of {@link VEvent events} that share the same {@link Uid uid}.
 */
@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public final class VEventGroup {

    private final CalendarQuery query;
    private final String name;
    private final List<VEvent> events;

    VEventGroup(CalendarQuery query, String name, List<VEvent> events) {
        this.query = query;
        this.name = name;
        this.events = List.copyOf(events);
        if (this.events.isEmpty()) throw new IllegalArgumentException("Empty VEventGroup");
    }

    /**
     * Gets the {@link CalendarQuery calendar query} that was used to create this {@link VEventGroup event group}.
     */
    public CalendarQuery query() {
        return this.query;
    }

    /**
     * Gets a list of all the {@link VEvent events} in this {@link VEventGroup event group}.
     */
    public List<VEvent> events() {
        return this.events;
    }

    /**
     * Gets the {@link DateStart start date} of the earliest {@link VEvent event} in this event group when using the
     * provided {@link SequenceBehavior sequence behaviour}.
     */
    public ICalDate startDate(SequenceBehavior behavior) {
        return this.query().startDate(this.startDateEvent(behavior));
    }

    /**
     * Gets the {@link TimeZone timezone} of the earliest {@link VEvent event} in this event group when using the
     * provided {@link SequenceBehavior sequence behaviour}.
     */
    public TimeZone timezone(SequenceBehavior behavior) {
        return this.query().timezone(this.startDateEvent(behavior));
    }

    private VEvent startDateEvent(SequenceBehavior behavior) {
        EventSelection sel = this.selectEvents(behavior);
        Optional<VEvent> earliestSelectedEvent = Stream.concat(sel.mainEvents().stream(), sel.replacementEvents().stream())
                .max(Comparator.comparing(this.query::startDate));
        if (earliestSelectedEvent.isPresent()) return earliestSelectedEvent.get();
        Optional<VEvent> earliestEvent = this.events.stream()
                .max(Comparator.comparing(this.query::startDate));
        if (earliestEvent.isEmpty()) {
            throw new RuntimeException("Don't reflectively mess with me!"); // Won't happen unless someone does something funky.
        }
        return earliestEvent.get();
    }

    @Override
    public String toString() {
        return "VEventGroup(" + this.name + ")";
    }

    /**
     * Returns a possibly-infinite stream of all {@link VEventInstance instances} of this {@link VEventGroup event group}.
     */
    public Stream<VEventInstance> instances(SequenceBehavior behavior) {
        return this.getInstances(behavior, Optional.empty());
    }

    /**
     * Returns a possibly-infinite stream of all {@link VEventInstance instances} of this {@link VEventGroup event group}.
     *
     * @param recurrenceStart The point in time from which on the recurrence shall be retrieved.
     */
    public Stream<VEventInstance> instances(SequenceBehavior behavior, Instant recurrenceStart) {
        return this.getInstances(behavior, Optional.of(recurrenceStart));
    }

    Stream<VEventInstance> getInstances(SequenceBehavior behavior, Optional<Instant> recurrenceStart) {
        EventSelection sel = this.selectEvents(behavior);

        Map<ICalDate, List<VEvent>> replacements = new HashMap<>();
        Map<VEvent, String> ambiguityCodes = new HashMap<>(); // In COEXIST mode, we need an additional value to disambiguate between events with the same uid and sequence
        for (VEvent replacement : sel.replacementEvents()) {
            ICalDate nominalStartDate = Objects.requireNonNull(replacement.getRecurrenceId().getValue());
            if (!sel.allowMultipleReplacements()) {
                // Filter multiple replacements on the same nominal start date by sequence order (and therefore DTSTAMP)
                if (!replacements.containsKey(nominalStartDate) || TimeHelper.SEQUENCE_ORDER.compare(replacements.get(nominalStartDate).getFirst(), replacement) < 0) {
                    replacements.put(nominalStartDate, List.of(replacement));
                }
            } else {
                List<VEvent> list = replacements.computeIfAbsent(nominalStartDate, k -> new ArrayList<>());
                list.add(replacement);
                if (list.size() >= 2) for (VEvent ambiguousEvent : list) {
                    String code = String.format("%08x", ambiguousEvent.hashCode()) + "+" + Base64.getUrlEncoder().encodeToString(this.query.summary(ambiguousEvent).getBytes(StandardCharsets.UTF_8));
                    ambiguityCodes.put(ambiguousEvent, code);
                }
            }
        }

        List<Stream<VEventInstance>> mainInstanceStreams = new ArrayList<>(); // The event streams from the main event and all THISANDFUTURE replacements without replacement filtering.
        List<Stream<VEventInstance>> recurrenceInstanceStreams = new ArrayList<>(); // Streams of all explicit recurrence events and their repetitions.
        Stream<VEventInstance> currentInfiniteStream = null;
        if (sel.supportsThisAndFuture()) {
            VEvent mainEvent = sel.mainEvents().getFirst();
            currentInfiniteStream = this.query.getRecurrences(mainEvent, recurrenceStart)
                    .map(date -> this.eventInstance(sel, ambiguityCodes, mainEvent, date, date, -1));
        }

        for (ICalDate replacementDate : replacements.keySet().stream().sorted(TimeHelper.timezoneSorting(this.timezone(behavior))).toList()) {
            List<VEvent> replacementList = replacements.get(replacementDate);
            for (VEvent replacement : replacementList) {
                recurrenceInstanceStreams.add(StreamHelper.indexed(this.query.getRecurrences(replacement, recurrenceStart))
                        .map(entry -> this.eventInstance(sel, ambiguityCodes, replacement, entry.getValue(), replacementDate, entry.getKey()))
                );
            }
            if (replacementList.size() == 1) {
                VEvent mainReplacement = replacementList.getFirst();
                if (currentInfiniteStream != null && mainReplacement.getRecurrenceId() instanceof RecurrenceId rid && Objects.equals(rid.getRange(), Range.THIS_AND_FUTURE)) {
                    mainInstanceStreams.add(currentInfiniteStream.takeWhile(instance -> instance.nominalStartDate().compareTo(replacementDate) < 0));

                    Duration replacementShift = Duration.diff(replacementDate, this.query.startDate(mainReplacement));
                    Optional<Instant> adjustedRecurrenceStart = recurrenceStart.map(instant -> instant.plusMillis(Math.min(0, replacementShift.toMillis())));

                    currentInfiniteStream = this.query.getRecurrences(sel.mainEvents().getFirst(), adjustedRecurrenceStart)
                            .dropWhile(nominalStartDate -> nominalStartDate.compareTo(replacementDate) < 0)
                            .map(nominalStartDate -> this.eventInstance(sel, ambiguityCodes, mainReplacement, shift(nominalStartDate, replacementShift), nominalStartDate, -1));
                }
            }
        }
        if (currentInfiniteStream != null) {
            mainInstanceStreams.add(currentInfiniteStream);
        } else {
            mainInstanceStreams.addAll(sel.mainEvents().stream().map(event -> this.query.getRecurrences(event, recurrenceStart)
                    .map(date -> this.eventInstance(sel, ambiguityCodes, event, date, date, -1))
            ).toList());
        }

        // Filter out the replaced events from the main streams and construct a list of all final streams
        Set<ICalDate> replacedRecurrences = Set.copyOf(replacements.keySet());
        List<Stream<VEventInstance>> allStreams = Stream.concat(
                mainInstanceStreams.stream().map(stream -> stream.filter(instance -> !replacedRecurrences.contains(instance.nominalStartDate()))),
                recurrenceInstanceStreams.stream()
        ).toList();

        Stream<VEventInstance> finalStream = StreamHelper.mergeOrderedStreams(allStreams, TimeHelper.CHRONOLOGICAL);
        if (recurrenceStart.isPresent()) {
            // If THISANDFUTURE replacements were used, the final stream may contains events before recurrenceStart. Filter them out.
            return finalStream.filter(instance -> !recurrenceStart.get().isAfter(TimeHelper.toInstant(instance.startDate(), this.query.timezone(instance.event()), false)));
        } else {
            return finalStream;
        }
    }

    private static ICalDate shift(ICalDate date, Duration duration) {
        return new ICalDate(new Date(date.getTime() + duration.toMillis()), date.hasTime());
    }

    private record EventSelection(List<VEvent> mainEvents, List<VEvent> replacementEvents, boolean trivialSequence,
                                  boolean supportsThisAndFuture, boolean allowMultipleReplacements) {

        private EventSelection(List<VEvent> mainEvents, List<VEvent> replacementEvents, boolean trivialSequence, boolean supportsThisAndFuture, boolean allowMultipleReplacements) {
            this.mainEvents = List.copyOf(mainEvents);
            this.replacementEvents = List.copyOf(replacementEvents);
            this.trivialSequence = trivialSequence && this.mainEvents.size() == 1;
            this.supportsThisAndFuture = supportsThisAndFuture && !allowMultipleReplacements && this.mainEvents.size() == 1;
            this.allowMultipleReplacements = allowMultipleReplacements;
        }
    }

    private EventSelection selectEvents(SequenceBehavior behavior) {
        Objects.requireNonNull(behavior);
        if (behavior == SequenceBehavior.UNIFORM || behavior == SequenceBehavior.OBSOLETE) {
            VEvent mainEvent = this.events().stream()
                    .filter(event -> event.getRecurrenceId() == null)
                    .max(TimeHelper.SEQUENCE_ORDER).orElse(null);
            if (mainEvent == null) {
                if (behavior == SequenceBehavior.UNIFORM) {
                    throw new IllegalStateException("iCalendar event with only replacement events: " + this.name);
                }
                return new EventSelection(List.of(), List.of(), true, false, false);
            }
            int sequenceNumber = Optional.ofNullable(mainEvent.getSequence()).map(seq -> Objects.requireNonNull(seq.getValue())).orElse(0);
            List<VEvent> allReplacements = this.events().stream()
                    .filter(event -> event.getRecurrenceId() != null)
                    .filter(event -> Optional.ofNullable(event.getSequence()).map(seq -> Objects.requireNonNull(seq.getValue())).orElse(0) == sequenceNumber)
                    .toList();
            if (behavior == SequenceBehavior.UNIFORM) {
                List<VEvent> unselectedEvents = this.events().stream()
                        .filter(event -> Optional.ofNullable(event.getSequence()).map(seq -> Objects.requireNonNull(seq.getValue())).orElse(0) != sequenceNumber)
                        .toList();
                if (!unselectedEvents.isEmpty()) {
                    throw new IllegalStateException("iCalendar obsoleted by sequence number: " + this.name + "@" + sequenceNumber);
                }
            }
            return new EventSelection(List.of(mainEvent), allReplacements, true, true, false);
        } else if (behavior == SequenceBehavior.ISOLATE) {
            Map<Optional<ICalDate>, List<VEvent>> eventIsolationMap = new HashMap<>();
            for (VEvent event : this.events()) {
                Optional<ICalDate> key = Optional.ofNullable(event.getRecurrenceId()).map(rid -> Objects.requireNonNull(rid.getValue()));
                eventIsolationMap.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
            }
            Optional<VEvent> mainEvent = eventIsolationMap.getOrDefault(Optional.<ICalDate>empty(), List.of()).stream().max(TimeHelper.SEQUENCE_ORDER);
            List<VEvent> replacementEvents = eventIsolationMap.entrySet().stream()
                    .filter(entry -> entry.getKey().isPresent())
                    .map(Map.Entry::getValue)
                    .flatMap(list -> list.stream().max(TimeHelper.SEQUENCE_ORDER).stream())
                    .toList();
            return new EventSelection(mainEvent.stream().toList(), replacementEvents, mainEvent.isPresent(), false, false);
        } else if (behavior == SequenceBehavior.COEXIST) {
            List<VEvent> mainEvents = this.events().stream().filter(event -> event.getRecurrenceId() == null).toList();
            List<VEvent> replacementEvents = this.events().stream().filter(event -> event.getRecurrenceId() != null).toList();
            return new EventSelection(mainEvents, replacementEvents, mainEvents.size() == 1, false, true);
        } else {
            throw new IllegalStateException("Unknown sequence behavior: " + behavior);
        }
    }

    private VEventInstance eventInstance(EventSelection sel, Map<VEvent, String> ambiguityCode, VEvent event, ICalDate startDate, ICalDate nominalStartDate, int nestedRecurrenceIndex) {
        StringBuilder sb = new StringBuilder();
        if (event.getUid() instanceof Uid evUid) {
            sb.append(Objects.requireNonNull(evUid.getValue())).append("#");
        } else if (event.getSummary() instanceof Summary summary) {
            sb.append(Objects.requireNonNull(summary.getValue()).hashCode()).append("H");
        } else {
            sb.append(event.hashCode()).append("X");
        }
        boolean isMainEvent = nestedRecurrenceIndex == -1;
        boolean isFirstIteration = Objects.equals(this.query().startDate(event), nominalStartDate);
        if (!sel.trivialSequence() || (sel.allowMultipleReplacements() && !isMainEvent)) {
            if (event.getSequence() instanceof Sequence seq) {
                sb.append(Objects.requireNonNull(seq.getValue())).append("S");
            } else {
                sb.append("0S");
            }
        }
        if (!isMainEvent || !isFirstIteration) {
            sb.append(TimeHelper.getTimeId(nominalStartDate)).append("T");
        }
        if (ambiguityCode.containsKey(event)) {
            sb.append(ambiguityCode.get(event)).append("+");
        }
        if (nestedRecurrenceIndex > 0) {
            sb.append(nestedRecurrenceIndex).append("R");
        }
        String id = sb.toString();
        return new VEventInstance(id, this.query, event, startDate, nominalStartDate);
    }
}
