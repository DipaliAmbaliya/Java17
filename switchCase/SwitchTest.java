// javac --enable-preview --release 17 SwitchTest.java
// java --enable-preview SwitchTest
public class SwitchTest {
    static float getFloat(Object o) {
        return switch (o) {
            case Integer i -> i.floatValue();
            case Double d -> d.floatValue();
            case String s && s.length() > 0 -> Float.parseFloat(s);
            case Test t -> t.getFloat(t.val);
            default -> 0f;
        };
    }

    public static void main(String[] args) {
        System.out.println("Integer : "+getFloat(12));
        System.out.println("Double : "+getFloat(12.33d));
        System.out.println("String : "+getFloat("22.32"));
        System.out.println("String : "+getFloat(""));
        Test test = new Test();
        test.val = String.valueOf(5);
        System.out.println("test : "+getFloat(test));
    }
}

class Test {
    String val;
    public float getFloat(String val) {
        return Float.parseFloat(val)*2;
    }
}