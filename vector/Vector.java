import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
// java -Djava.library.path=/lib/x86_64-linux-gnu --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector Vector.java

public class Vector {
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    public static void main(String args[]) {
        float[] a = new float[] {1f, 2f, 3f, 4f};
        float[] b = new float[] {5f, 8f, 10f, 12f};
        float[] c = new float[10_000];
        commonVectorComputation(a,b,c);
        newVectorComputation(a,b,c);
    }

    public static void newVectorComputation(float[] a, float[] b, float[] c) {
        for (var i = 0; i < a.length; i += SPECIES.length()) {
            var m = SPECIES.indexInRange(i, a.length);
            var va = FloatVector.fromArray(SPECIES, a, i, m);
            var vb = FloatVector.fromArray(SPECIES, b, i, m);
            var vc = va.mul(vb);
            vc.intoArray(c, i, m);
            System.out.println(vc);
        }

    }

    public static void commonVectorComputation(float[] a, float[] b, float[] c) {
        for (var i = 0; i < a.length; i ++) {
            c[i] = a[i] * b[i];
            System.out.println(c[i]);
        }

    }
}
