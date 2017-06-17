public class Cubic {

    double a, b, c, d;         /* a + b*u + c*u^2 +d*u^3 */

    public Cubic(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /**
     * evaluate cubic
     */
    public double eval(double x) {
        return (((d * x) + c) * x + b) * x + a;
    }
}
