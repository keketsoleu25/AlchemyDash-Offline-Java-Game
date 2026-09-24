import java.util.Random;

public final class GameObject {
    public enum Type { PLAYER, OBSTACLE, RING }
    public Type type;
    public Vector3D position;
    public Mesh3D mesh;
    public double halfWidth, halfHeight, halfDepth;
    public boolean active = true;
    public int shape;

    public GameObject(Type type, Vector3D position, int shape) {
        this.type = type;
        this.position = position;
        this.shape = shape;
        rebuildMesh();
    }

    private void rebuildMesh() {
        if (type == Type.RING) {
            halfWidth = halfHeight = 2.65;
            halfDepth = .45;
            mesh = Mesh3D.ring(position, 2.65);
        } else if (type == Type.PLAYER) {
            halfWidth = .85; halfHeight = .7; halfDepth = 1;
            mesh = Mesh3D.pyramid(position, 1.5);
        } else {
            halfWidth = halfHeight = halfDepth = shape == 0 ? 1.45 : 1.65;
            mesh = shape == 0 ? Mesh3D.cube(position, 2.9) : Mesh3D.pyramid(position, 3.3);
        }
    }

    public void update(double dt) {
        if (type == Type.OBSTACLE) mesh.rotate(dt*.29, dt*.68);
        if (type == Type.RING) mesh.rotate(0, dt*.11);
    }

    public void recycle(Random random, double cameraZ) {
        double nx = (random.nextInt(5)-2)*4.5;
        double ny = (random.nextInt(3)-1)*3.4;
        position = new Vector3D(nx,ny,cameraZ+400+random.nextDouble()*65);
        if (type == Type.OBSTACLE) shape = random.nextBoolean() ? 0 : 1;
        rebuildMesh();
        active = true;
    }

    public boolean overlaps(Vector3D player) {
        return Math.abs(position.x-player.x)<halfWidth+.65
            && Math.abs(position.y-player.y)<halfHeight+.55
            && Math.abs(position.z-player.z)<halfDepth+1.2;
    }

    public boolean insideRing(Vector3D player) {
        double dx=player.x-position.x, dy=player.y-position.y;
        return dx*dx+dy*dy < 1.85*1.85;
    }
}
