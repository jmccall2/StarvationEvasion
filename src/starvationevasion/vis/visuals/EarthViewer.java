package starvationevasion.vis.visuals;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import starvationevasion.common.MapPoint;
import starvationevasion.io.XMLparsers.GeographyXMLparser;
import starvationevasion.sim.GeographicArea;
import starvationevasion.vis.ClientTest.CustomLayout;
import starvationevasion.vis.controller.SimParser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;


/**
 * http://stackoverflow.com/questions/19621423/javafx-materials-bump-and-spec-maps
 * Original Author:jewelsea,http://stackoverflow.com/users/1155209/jewelsea
 * Modified by:Tess Daughton
 **/


public class EarthViewer {

  private static final double ROTATE_SECS = 30;

  private static final double MAP_WIDTH = 8192 / 2d;
  private static final double MAP_HEIGHT = 4092 / 2d;

  private final double MINI_EARTH_RADIUS;
  private final double LARGE_EARTH_RADIUS;

  private double zoomPosition = 0;
  private final DoubleProperty angleX = new SimpleDoubleProperty(0);
  private final DoubleProperty angleY = new SimpleDoubleProperty(0);
  double anchorX, anchorY;
  private double anchorAngleX = 0;
  private double anchorAngleY = 0;
  private boolean showOverlay = true;  //show all country overlay
  private boolean showClouds = true;  //show cloud overlay

  private static final String DIFFUSE_MAP = "visResources/DIFFUSE_MAP.jpg";//"vis_resources/DIFFUSE_MAP.jpg";
  //"http://www.daidegasforum.com/images/22/world-map-satellite-day-nasa-earth.jpg";
  private static final String NORMAL_MAP = "visResources/NORMAL_MAP.jpg";//"vis_resources/NORMAL_MAP.jpg";
  //"http://planetmaker.wthr.us/img/earth_normalmap_flat_8192x4096.jpg";
  private static final String SPECULAR_MAP = "visResources/SPEC_MAP.jpg";//"vis_resources/SPEC_MAP.jpg";
  //"http://planetmaker.wthr.us/img/earth_specularmap_flat_8192x4096.jpg";
  private static final String OUTLINE_MAP = "visResources/WorldMapOutline.png";//"vis_resources/SPEC_MAP.jpg";
  private static final String REGION_OVERLAY = "visResources/WorldMapRegionsFull8x6.png";//"vis_resources/SPEC_MAP.jpg";

  private static Group largeEarth;
  private static Group miniEarth;
  private String regionTitle;

  private Sphere earth;
  private Sphere overlay;
  private Sphere cloud;
  private PhongMaterial earthMaterial;
  private CustomLayout layoutPanel;

  public EarthViewer(double smallEarthRadius, double largeEarthRadius, CustomLayout l) {
    MINI_EARTH_RADIUS = smallEarthRadius;
    LARGE_EARTH_RADIUS = largeEarthRadius;
    miniEarth = buildScene(MINI_EARTH_RADIUS);
    largeEarth = buildScene(LARGE_EARTH_RADIUS);
    layoutPanel = l;
  }

  public Group buildScene(double earthRadius)
  {
    earth = new Sphere(earthRadius);
    earthMaterial = new PhongMaterial();
    /* Material */
    earthMaterial.setDiffuseMap
        (new Image(getClass().getClassLoader().getResourceAsStream(DIFFUSE_MAP), MAP_WIDTH, MAP_HEIGHT, true, true));
//    earthMaterial.setBumpMap
//        (new Image(getClass().getClassLoader().getResourceAsStream(NORMAL_MAP), MAP_WIDTH, MAP_HEIGHT, true, true));
//    earthMaterial.setSpecularMap
//            (new Image(getClass().getClassLoader().getResourceAsStream(SPECULAR_MAP), MAP_WIDTH, MAP_HEIGHT, true, true));
//    earthMaterial.setSelfIlluminationMap
//            (new Image(getClass().getClassLoader().getResourceAsStream(REGION_OVERLAY), MAP_WIDTH, MAP_HEIGHT, true, true));

    earth.setMaterial(earthMaterial);
    return new Group(earth);
  }

  /*Overlay for any events needing to be displayed on the globe**/
  private void buildOverlay()
  {
    overlay = new Sphere(LARGE_EARTH_RADIUS);
    final PhongMaterial cloudMaterial = new PhongMaterial();
    cloudMaterial.setDiffuseMap
            (new Image(getClass().getClassLoader().getResourceAsStream(REGION_OVERLAY), MAP_WIDTH, MAP_HEIGHT, true, true));
    overlay.setMaterial(cloudMaterial);
    largeEarth.getChildren().addAll(overlay);
  }

  /*Clouds to be displayed on the globe**/
  private void buildClouds()
  {
    cloud = new Sphere(LARGE_EARTH_RADIUS*1.05);
    final PhongMaterial cloudMaterial = new PhongMaterial();
//    cloudMaterial.setDiffuseMap
//            (new Image(getClass().getClassLoader().getResourceAsStream(CLOUD_OVERLAY), MAP_WIDTH, MAP_HEIGHT, true, true));
    cloud.setMaterial(cloudMaterial);
    largeEarth.getChildren().addAll(cloud);
  }

  /**
   * Used by Client GUI to toggle between Earth Viewing Modes
   *
   * @return Large Earth Group to be attached in client's layout
   */
  public Group getLargeEarth() {
    return largeEarth;
  }


  /**
   * Used by Client GUI to toggle between Earth Viewing Modes
   *
   * @return Mini Earth Group to be attached in client's layout
   */
  public Group getMiniEarth() {
    return miniEarth;
  }

  /**
   * Runs a continuous animation of the Earth rotating around its y-axis
   * Used for Mini Earth Mode in client GUI
   */
  public void startRotate() {
    rotateAroundYAxis(miniEarth).play();
  }

  public void startEarth() {
    /*request focus to listen to key presseseses*/
    largeEarth.requestFocus();

    /* Init group */
    Rotate groupXRotate, groupYRotate;
    largeEarth.getTransforms().setAll(
            groupXRotate = new Rotate(0, Rotate.X_AXIS),
            groupYRotate = new Rotate(0, Rotate.Y_AXIS)
    );
    groupXRotate.angleProperty().bind(angleX);
    groupYRotate.angleProperty().bind(angleY);

    largeEarth.setOnMouseDragged((MouseEvent event) -> {
      angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
      angleY.set(anchorAngleY + anchorX - event.getSceneX());
    });
    largeEarth.setOnMousePressed((MouseEvent event) -> {
      anchorX = event.getSceneX();
      anchorY = event.getSceneY();
      anchorAngleX = angleX.get();
      anchorAngleY = angleY.get();
      PickResult pickResult = event.getPickResult();

      /* Pick point on texture to derive lat long from java x y axis */
      Point2D point = pickResult.getIntersectedTexCoord(); //in percentages
      double lat = (point.getY() - 0.5) * -180;
      double lon = (point.getX() - 0.5) * 360;
      new SimParser(lat, lon, this);
      layoutPanel.title.setFont(Font.font ("Times", 20));
      layoutPanel.title.setText(getRegionTitle());
    });

    /**setTranslate can be used to zoom in and out on the world*/
    largeEarth.setOnScroll(event ->
    {
      if (event.getDeltaY() < 0 && zoomPosition > -840)
      {
        largeEarth.setTranslateZ(zoomPosition -= 10);
      }
      else if (event.getDeltaY() > 0 && zoomPosition < 500)
      {
        largeEarth.setTranslateZ(zoomPosition += 10);
      }
    });

    largeEarth.setOnKeyPressed(event->
      {
        switch (event.getCode())
        {

          case P:
            //currently I cannot find a stop for this animation
              rotateAroundYAxis(largeEarth).playFromStart();
            break;

          case R:
            if(showOverlay)
            {
              buildOverlay();
              showOverlay = false;
            }
            else
            {
              largeEarth.getChildren().remove(overlay);
              showOverlay = true;
            }
            break;

          case C:
            if(showClouds)
            {
              buildClouds();
              showClouds = false;
            }
            else
            {
              largeEarth.getChildren().remove(cloud);
              showClouds = true;
            }
            break;


        }
      });
  }


  public void startRotate(Group group)
  {
    rotateAroundYAxis(group).play();
  }

  private RotateTransition rotateAroundYAxis(Node node)
  {
    RotateTransition rotate = new RotateTransition(Duration.seconds(ROTATE_SECS), node);
    rotate.setAxis(Rotate.Y_AXIS);
    rotate.setFromAngle(360);
    rotate.setToAngle(0);
    rotate.setInterpolator(Interpolator.LINEAR);
    rotate.setCycleCount(RotateTransition.INDEFINITE);

    return rotate;
  }

  public static void main(String args[])
  {
    int scale = 50;
    BufferedImage i = new BufferedImage(360 * scale, 180 * scale, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = i.createGraphics();
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(0, 0, 360 * scale, 180 * scale);
    g.setColor(Color.BLACK);

    Collection<GeographicArea> modelGeography = new GeographyXMLparser().getGeography();
    for (GeographicArea a : modelGeography) {
      Polygon poly = new Polygon();
      for (MapPoint p : a.getPerimeter()) {
        int latitude = (int) ((p.latitude - 90) * -1 * scale);
        int longitude = (int) ((p.longitude + 180) * scale);
        poly.addPoint(longitude, latitude);
      }
      g.setComposite(AlphaComposite.Src);
      g.setColor(Color.RED);
      g.drawPolygon(poly);
    }

    try {
      ImageIO.write(i, "PNG", new File("/Users/laurencemirabal/Desktop/test.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Done!");
    g.dispose();
  }

  public void setRegionTitle(String s)
  {
    regionTitle = s;
  }

  public String getRegionTitle()
  {
    return regionTitle;
  }


}

