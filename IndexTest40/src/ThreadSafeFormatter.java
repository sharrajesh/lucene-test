import java.text.DecimalFormat;

class ThreadSafeFormatter {
  private static final ThreadLocal<DecimalFormat> DecimalFormatHolder = new ThreadLocal<DecimalFormat>() {
    @Override
    protected DecimalFormat initialValue() {
      return new DecimalFormat("#,###");
    }
  };
  
  private static final ThreadLocal<DecimalFormat> DoubleFormatHolder = new ThreadLocal<DecimalFormat>() {
    @Override
    protected DecimalFormat initialValue() {
      return new DecimalFormat("#,###.00");
    }
  };
  
  public static String Format(long value) {
    return DecimalFormatHolder.get().format(value);
  }
  
  public static String Format(int value) {
    return DecimalFormatHolder.get().format(value);
  }
  
  public static String Format(double value) {
    return DoubleFormatHolder.get().format(value);
  }
}
