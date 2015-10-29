package ch.ethz.inf.vs.vsvvenzinchat;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VectorClock implements Clock {

    /**
     * Associate a process id with a logical time
     */
    public Map<Integer, Integer> vector;

   public  VectorClock() {
        vector = new TreeMap<>();
    }

    @Override
    public void update(Clock other) {
        VectorClock o = (VectorClock) other;
        VectorClock vC = new VectorClock();

        // Store all pairs of pids and times in another Map
        Map<Integer, Integer> values = new TreeMap<>();

        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(o.toString());
        while (m.find()) {
            int pid = Integer.parseInt(m.group());
            //noinspection ResultOfMethodCallIgnored
            m.find();
            int t = Integer.parseInt(m.group());
            values.put(pid, t);
        }

        for(int i : vector.keySet()) {
            if (values.containsKey(i)) {
                int t = values.get(i);
                if (t < getTime(i)) {
                    values.put(i, getTime(i));
                }
            } else {
                values.put(i, getTime(i));
            }
        }

        // Add the new values to the new Clock and set the clock
        for (int pid : values.keySet()) {
            vC.addProcess(pid, values.get(pid));
        }
        this.setClock(vC);
    }

    @Override
    public void setClock(Clock other) {
        VectorClock o = (VectorClock) other;
        vector.clear();

        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(o.toString());
        while (m.find()) {
            int pid = Integer.parseInt(m.group());
            //noinspection ResultOfMethodCallIgnored
            m.find();
            int t = Integer.parseInt(m.group());
            vector.put(pid, t);
        }
    }

    @Override
    public void tick(Integer pid) {
        int t = getTime(pid);
        vector.remove(pid);
        addProcess(pid, t + 1);
    }

    @Override
    public boolean happenedBefore(Clock other) {
        VectorClock o = (VectorClock) other;

        for(int i : vector.keySet()) {
            if(getTime(i) > o.getTime(i)) {
               return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        int count = 1;
        for(int i : vector.keySet()) {
            sb.append('"');
            sb.append(i);
            sb.append('"');
            sb.append(':');
            sb.append(getTime(i));
            if (count < vector.size())
                sb.append(',');
            count++;
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void setClockFromString(String clock) {
        if (clock.equals("{}")) {
            vector = new TreeMap<>();
            return;
        }
        try {
            VectorClock vC = new VectorClock();
            Pattern p = Pattern.compile("-?\\d+");
            Matcher m = p.matcher(clock);
            while (m.find()) {
                int pid = Integer.parseInt(m.group());
                //noinspection ResultOfMethodCallIgnored
                m.find();
                int t = Integer.parseInt(m.group());
                vC.addProcess(pid, t);
            }

            this.setClock(vC);
        } catch (Exception e) {
            // Do nothing in case there is no correct String
        }
    }

    /**
     * Get the current clock value.
     *
     * @param pid process id
     * @return current clock value
     */
    public int getTime(Integer pid) {
        return vector.get(pid);
    }

    /**
     * Add a new process and its vector clock to the current clock.
     *
     * @param pid process id
     * @param time clock value for the process
     */
    public void addProcess(Integer pid, int time) {
        vector.put(pid, time);
    }
}
