package org.aedificatores.libspeedydoge.VF.Objects;

import org.aedificatores.libspeedydoge.Universal.Math.Pose;
import org.aedificatores.libspeedydoge.Universal.Math.Vector2;
import org.aedificatores.libspeedydoge.Universal.UniversalConstants;
import org.aedificatores.libspeedydoge.VF.Boundary;
import org.aedificatores.libspeedydoge.VF.Objects.Robot;

/**
 * Generates an obstacle which prevents vector fields from pushing past the field wall
 **/
public class FieldWall implements Boundary {
    private boolean isActive;

    public Vector2 interact(Pose point, Vector2 vector) {
        Robot robot = UniversalConstants.getRobot(point);
        if (isActive) {
            double effectiveRadius = 9;

            if (point.x > 72 - effectiveRadius && vector.x > 0)
                vector.x = 0;
            if (point.x < -72 + effectiveRadius && vector.x < 0)
                vector.x = 0;
            if (point.y > 72 - effectiveRadius && vector.y > 0)
                vector.y = 0;
            if (point.y < -72 + effectiveRadius && vector.y < 0)
                vector.y = 0;
        }
        return vector;
    }

    public void activate(){
        isActive = true;
    }
    public void deactivate(){
        isActive = false;
    }
    public boolean isActive(){
        return isActive;
    }
}