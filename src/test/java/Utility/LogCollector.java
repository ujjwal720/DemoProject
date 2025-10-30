package Utility;

import java.util.ArrayList;
import java.util.List;

public class LogCollector {

    private static ThreadLocal<List<String>> logs = ThreadLocal.withInitial(ArrayList::new);

    public static void addLog(String message) {
        logs.get().add(message);
    }

    public static List<String> getLogs() {
        return new ArrayList<>(logs.get()); // return copy
    }

    public static void clear() {
        logs.remove(); // reset after test
    }
}
