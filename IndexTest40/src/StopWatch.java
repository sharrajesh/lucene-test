public class StopWatch {
  private long StartTime = -1;
  private long StopTime = -1;
  private boolean Running = false;
  private long Elapsed = 0;
  
  public StopWatch Start() {
    StartTime = System.currentTimeMillis();
    Running = true;
    return this;
  }
  
  public StopWatch Stop() {
    StopTime = System.currentTimeMillis();
    Elapsed += (StopTime - StartTime);
    Running = false;
    return this;
  }
  
  public long GetElapsed() {
    if (StartTime == -1) {
      return 0;
    }
    if (Running)
      return Elapsed + (System.currentTimeMillis() - StartTime);
    else
      return Elapsed;
  }
  
  public StopWatch Reset() {
    StartTime = -1;
    StopTime = -1;
    Elapsed = 0;
    Running = false;
    return this;
  }
}
