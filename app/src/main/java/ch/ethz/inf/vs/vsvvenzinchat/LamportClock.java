package ch.ethz.inf.vs.vsvvenzinchat;

public class LamportClock implements Clock {

    private int time;

    @Override
    public void update(Clock other) {
        LamportClock o = (LamportClock) other;
        if(getTime() < o.getTime()) {
            setClock(other);
        }
    }

    @Override
    public void setClock(Clock other) {
        LamportClock o = (LamportClock) other;
        setTime(o.getTime());
    }

    @Override
    public void tick(Integer pid) {
        setTime(getTime() + 1);
    }

    @Override
    public boolean happenedBefore(Clock other) {
        LamportClock o = (LamportClock) other;
        return getTime() < o.getTime();
    }

    @Override
    public String toString() {
        int t = getTime();
        return Integer.toString(t);
    }

    @Override
    public void setClockFromString(String clock) {
        try {
            time = Integer.valueOf(clock);
        } catch (Exception e) {
            // Do nothing in case there is no valid integer
        }
    }

    /**
     *  Override the current clock value.
     *
     *  @param time: value to override the clock
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     *  Get the current clock value.
     *
     *  @return the current clock value
     */
    public int getTime() {
        return time;
    }
}
