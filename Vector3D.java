public final class Vector3D {
    public double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }

    public Vector3D rotateX(double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vector3D(x, y * c - z * s, y * s + z * c);
    }

    public Vector3D rotateY(double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vector3D(x * c + z * s, y, -x * s + z * c);
    }
}
