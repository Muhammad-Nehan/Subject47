package subject47.level;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;

public class CameraSystem {

    private SimpleApplication app;
    private int currentCam = 0;

    private Vector3f[] cameraPositions = {
            new Vector3f(5,5,5),
            new Vector3f(10,5,10),
            new Vector3f(2,5,12)
    };

    public CameraSystem(SimpleApplication app) {
        this.app = app;
    }

    public void switchCamera() {

        currentCam = (currentCam + 1) % cameraPositions.length;

        app.getCamera().setLocation(cameraPositions[currentCam]);

        System.out.println("Switched camera to " + currentCam);

        if (currentCam == 2) {
            System.out.println("SHADOW SEEN IN CAMERA");
        }
    }
}