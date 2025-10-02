package eu.tuxtown.crocus.core.sync;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotNullByDefault
public class CalendarUpdater {

    public static void updateCalendar(Path calendarsPath, ConfiguredService<CalendarType<?, ?>, Calendar> calendar, Map<EventKey, Event> collectedEvents, boolean noIncremental) throws IOException {
        if (!Files.exists(calendarsPath)) Files.createDirectory(calendarsPath);
        Path path = calendarsPath.resolve(calendar.key().moduleName() + "-" + savePathPart(calendar.id()) + ".dat");
        Path pathBackup = calendarsPath.resolve(calendar.key().moduleName() + "-" + savePathPart(calendar.id()) + ".bak");

        StoredData data;
        if (noIncremental) {
            Crocus.info("Performing non-incremental update: Clearing calendar.");
            CrocusRuntime.get().increaseLogLayer();
            try {
                OptionalInt deleted = calendar.value().deleteAllEvents();
                if (deleted.isPresent()) {
                    Crocus.info("Done. Deleted " + deleted.getAsInt() + " events.");
                } else {
                    Crocus.info("Done.");
                }
                data = StoredData.EMPTY;
            } catch (Exception e) {
                try {
                    if (Files.isRegularFile(path)) {
                        Files.copy(path, pathBackup, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception x) {
                    //
                }
                throw new RuntimeException("Non-incremental update failed: Could not clear calendar.", e);
            } finally {
                CrocusRuntime.get().decreaseLogLayer();
            }
        } else {
            try {
                data = StoredData.load(path);
            } catch (IOException e) {
                try {
                    if (Files.isRegularFile(path)) {
                        Files.move(path, pathBackup, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception x) {
                    //
                }
                throw e;
            }
        }

        Map<EventKey, String> updatingIdMap = new HashMap<>(data.idMap());

        // Register a shutdown hook that saves a failure state if Crocus is interrupted while updating the calendar.
        AtomicBoolean successful = new AtomicBoolean(false);
        Thread shutdownHook = new Thread(() -> {
            if (!successful.get()) {
                try {
                    trySaveFailureState(null, path, data, updatingIdMap);
                    System.err.println("Interrupted while updating calendar " + calendar.id() + ". Failure state written.");
                } catch (Exception e) {
                    System.err.println("Interrupted while updating calendar " + calendar.id() + ". Failure state could not be written. Calendar is probably broken.");
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            if (!data.eventsToDelete().isEmpty()) {
                Crocus.info("Calendar in inconsistent state: Deleting " + data.eventsToDelete().size() + " events.");
                CrocusRuntime.get().increaseLogLayer();
                try {
                    calendar.value().deleteEvents(data.eventsToDelete());
                    updatingIdMap.values().removeIf(s -> data.eventsToDelete().contains(s));
                } finally {
                    CrocusRuntime.get().decreaseLogLayer();
                }
            }

            Map<EventKey, Event> changedEvents = new HashMap<>();

            for (Map.Entry<EventKey, Event> entry : collectedEvents.entrySet()) {
                EventKey key = entry.getKey();
                Event event = entry.getValue();

                if (!data.allEvents().containsKey(key) || !Objects.equals(data.allEvents().get(key), event)) {
                    changedEvents.put(key, event);
                }
            }

            Set<EventKey> deletedEvents = data.allEvents().keySet().stream().filter(key -> !collectedEvents.containsKey(key)).collect(Collectors.toUnmodifiableSet());

            if (!data.eventsToDelete().isEmpty() || !changedEvents.isEmpty() || !deletedEvents.isEmpty()) {
                Crocus.info("Updating " + changedEvents.size() + " events (out of " + collectedEvents.size() + " total). Deleted events: " + deletedEvents.size());
                CrocusRuntime.get().increaseLogLayer();
                try {
                    Calendar.CalendarData calendarData = new Calendar.CalendarData(Collections.unmodifiableMap(changedEvents), deletedEvents);
                    Calendar.CalendarIds calendarIds = new CalendarIdsImpl(updatingIdMap);

                    calendar.value().updateEvents(calendarData, calendarIds);

                    // Need to remove deleted events from idMap after update as the update methods needs them in idMap to query the id for deletion.
                    updatingIdMap.keySet().removeAll(deletedEvents);
                } finally {
                    CrocusRuntime.get().decreaseLogLayer();
                }
            } else {
                Crocus.info("Calendar is up to date. (" + collectedEvents.size() + " total events)");
            }

            new StoredData(Set.of(), Map.copyOf(updatingIdMap), collectedEvents).save(path);
            successful.set(true);
        } catch (Exception e) {
            // Calendar update failed, we don't know the state of the calendar.
            // Mark all events for deletion next time.
            trySaveFailureState(e, path, data, updatingIdMap);
            throw e;
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    private static void trySaveFailureState(@Nullable Exception exc, Path path, StoredData oldData, Map<EventKey, String> updatingIdMap) throws IOException {
        // Calendar update failed, we don't know the state of the calendar.
        // Mark all events for deletion next time.
        try {
            Files.deleteIfExists(path);
        } catch (Exception x) {
            if (exc != null) {
                exc.addSuppressed(x);
            } else {
                throw new IOException("Failed to delete old calendar state file.");
            }
        }
        try {
            // Include old idMap entries as well as new idMap entries as we don't know what has already been processed.
            new StoredData(Stream.concat(
                    Stream.concat(oldData.eventsToDelete().stream(), oldData.idMap().values().stream()),
                    updatingIdMap.values().stream()
            ).collect(Collectors.toUnmodifiableSet()), Map.of(), Map.of()).save(path);
        } catch (Exception x) {
            if (exc != null) {
                exc.addSuppressed(x);
            } else {
                throw new IOException("Failed to save failure state file.");
            }
        }
    }

    private static String savePathPart(String id) {
        StringBuilder sb = new StringBuilder();
        for (char chr : id.toCharArray()) {
            if (chr >= 35 && chr <= 126 && chr != 42 && chr != 47 && chr != 58 && chr != 60 && chr != 62 && chr != 63 && chr != 92 && chr != 124) {
                sb.append(chr);
            } else {
                sb.append('!').append(String.format("%04X", (int) chr));
            }
        }
        return sb.toString();
    }

    private record StoredData(Set<String> eventsToDelete, Map<EventKey, String> idMap, Map<EventKey, Event> allEvents) {

        public static final StoredData EMPTY = new StoredData(Set.of(), Map.of(), Map.of());

        @SuppressWarnings("unchecked")
        public static StoredData load(Path path) throws IOException {
            if (!Files.exists(path)) return EMPTY;
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
                Set<String> allKeys = new HashSet<>();
                int allKeysLen = in.readInt();
                for (int i = 0; i < allKeysLen; i++) {
                    allKeys.add(in.readUTF());
                }

                try {
                    Set<String> eventsToDelete = (Set<String>) in.readObject();
                    Map<EventKey, String> idMap = (Map<EventKey, String>) in.readObject();
                    Map<EventKey, Event> allEvents = (Map<EventKey, Event>) in.readObject();
                    return new StoredData(Set.copyOf(eventsToDelete), Map.copyOf(idMap), Map.copyOf(allEvents));
                } catch (InvalidClassException e) {
                    // Some serial version uid has changed, delete everything and start over
                    return new StoredData(Set.copyOf(allKeys), Map.of(), Map.of());
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to load stored data from " + path, e);
            }
        }

        public void save(Path path) throws IOException {
            // First save to byte array, so we catch any serialisation exception before accessing the file system
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(out);

            // Write all currently existing event keys (as internal id) without object serialisation, so they can be used as fallback if
            // a serial version uid changes somewhere to get the calendar into a known state (by deleting all events)
            Set<String> allKeys = Stream.concat(this.eventsToDelete().stream(), this.idMap().values().stream()).collect(Collectors.toUnmodifiableSet());
            objectOut.writeInt(allKeys.size());
            for (String internalId : allKeys) {
                objectOut.writeUTF(internalId);
            }

            objectOut.writeObject(Set.copyOf(this.eventsToDelete()));
            objectOut.writeObject(Map.copyOf(this.idMap()));
            objectOut.writeObject(Map.copyOf(this.allEvents()));
            objectOut.close();
            Files.write(path, out.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private record CalendarIdsImpl(Map<EventKey, String> idMap) implements Calendar.CalendarIds {

        @Override
        @Nullable
        public String getId(EventKey key) {
            return this.idMap().get(key);
        }

        @Override
        public void setId(EventKey key, String id) {
            Objects.requireNonNull(key);
            if (this.idMap().containsKey(key)) {
                throw new IllegalStateException("Can't add internal id for known object: " + key + "\n  Known id associated with this object is " + this.idMap().get(key) + "\n  Newly requested id is " + id);
            }
            this.idMap().put(key, Objects.requireNonNull(id));
        }
    }
}
