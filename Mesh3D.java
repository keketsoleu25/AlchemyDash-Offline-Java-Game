public final class Mesh3D {
    public final Vector3D[] vertices;
    public final int[][] edges;
    public Vector3D position;
    public double pitch, yaw;

    private Mesh3D(Vector3D[] vertices, int[][] edges, Vector3D position) {
        this.vertices = vertices;
        this.edges = edges;
        this.position = position;
    }

    public Vector3D worldVertex(int index) {
        return vertices[index].rotateX(pitch).rotateY(yaw).add(position);
    }

    public void translate(Vector3D delta) {
        position = position.add(delta);
    }

    public void rotate(double pitchDelta, double yawDelta) {
        pitch += pitchDelta;
        yaw += yawDelta;
    }

    public static Mesh3D cube(Vector3D position, double size) {
        double h = size / 2.0;
        Vector3D[] v = {
            new Vector3D(-h,-h,-h), new Vector3D(h,-h,-h),
            new Vector3D(h,h,-h), new Vector3D(-h,h,-h),
            new Vector3D(-h,-h,h), new Vector3D(h,-h,h),
            new Vector3D(h,h,h), new Vector3D(-h,h,h)
        };
        int[][] e = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},
                     {0,4},{1,5},{2,6},{3,7}};
        return new Mesh3D(v,e,position);
    }

    public static Mesh3D pyramid(Vector3D position, double size) {
        double h = size / 2.0;
        Vector3D[] v = {new Vector3D(-h,-h,-h),new Vector3D(h,-h,-h),
            new Vector3D(h,-h,h),new Vector3D(-h,-h,h),new Vector3D(0,h,0)};
        int[][] e = {{0,1},{1,2},{2,3},{3,0},{0,4},{1,4},{2,4},{3,4}};
        return new Mesh3D(v,e,position);
    }

    public static Mesh3D ring(Vector3D position, double radius) {
        Vector3D[] v = new Vector3D[16];
        int[][] e = new int[24][2];
        for (int i=0;i<8;i++) {
            double angle = Math.PI * 2 * i / 8 + Math.PI / 8;
            double c = Math.cos(angle), s = Math.sin(angle);
            v[i] = new Vector3D(c*radius,s*radius,0);
            v[i+8] = new Vector3D(c*radius*.78,s*radius*.78,0);
            e[i] = new int[]{i,(i+1)%8};
            e[i+8] = new int[]{i+8,(i+1)%8+8};
            e[i+16] = new int[]{i,i+8};
        }
        return new Mesh3D(v,e,position);
    }
}
