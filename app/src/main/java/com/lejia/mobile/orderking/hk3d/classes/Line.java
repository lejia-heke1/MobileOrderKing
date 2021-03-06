package com.lejia.mobile.orderking.hk3d.classes;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Author by HEKE
 *
 * @time 2018/6/23 12:12
 * TODO: 线段对象
 */
public class Line implements Parcelable {

    public Point down; // 按下点(起始点)
    public Point up; // 弹起点(结尾点)
    public double thickess; // 厚度

    private ArrayList<AuxiliaryLine> auxiliaryLineList; // 带厚度的边线集合

    public Line(Point down, Point up) {
        this.down = down;
        this.up = up;
        this.thickess = 24.0d;
    }

    protected Line(Parcel in) {
        down = in.readParcelable(Point.class.getClassLoader());
        up = in.readParcelable(Point.class.getClassLoader());
        thickess = in.readDouble();
    }

    /**
     * 判断是否无效线段
     */
    public boolean invalid() {
        return down == null || up == null || down.equals(up);
    }

    /**
     * TODO 手动加载边线，用于墙体边线处理
     */
    public void loadAuxiliaryArray() {
        if (auxiliaryLineList == null) {
            auxiliaryLineList = new ArrayList<>();
        }
        auxiliaryLineList.clear();
        Point center = getCenter();
        if (center == null)
            return;
        ArrayList<Point> rotateList = PointList.getRotateVertexs(getAngle(), getThickess(), getLength(), getCenter());
        PointList pointList = new PointList(rotateList);
        ArrayList<Line> linesList = pointList.toLineList();
        if (linesList == null)
            return;
        for (Line line : linesList) {
            double length = line.getLength();
            if (Math.abs(length - thickess) >= 1.0d) {
                auxiliaryLineList.add(new AuxiliaryLine(line.down.copy(), line.up.copy()));
            }
        }
    }

    /**
     * 获取厚度
     */
    public double getThickess() {
        return thickess;
    }

    /**
     * 设置厚度
     *
     * @param thickess
     */
    public void setThickess(double thickess) {
        this.thickess = thickess;
        loadAuxiliaryArray();
    }

    /**
     * 判断线段是否垂直
     */
    public boolean isVertical() {
        double angle = getAngle();
        return angle == 90 || angle == 270;
    }

    /**
     * 判断线段是否水平
     */
    public boolean isHorizontal() {
        double angle = getAngle();
        return angle == 0 || angle == 180;
    }

    /**
     * 获取线段与水平线形成的角度,范围0-360
     */
    public double getAngle() {
        if (invalid())
            return 0d;
        double x = up.x - down.x;
        double y = up.y - down.y;
        double angle = Math.atan2(y, x) * 180.0d / Math.PI;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    /**
     * 获取线段的长度
     */
    public double getLength() {
        if (invalid())
            return 0d;
        double x = up.x - down.x;
        double y = up.y - down.y;
        double dist = Math.sqrt(x * x + y * y);
        return dist;
    }

    /**
     * 获取中心点
     */
    public Point getCenter() {
        if (invalid())
            return null;
        return new Point((down.x + up.x) / 2, (down.y + up.y) / 2);
    }

    /**
     * 获取边线列表
     */
    public ArrayList<AuxiliaryLine> getAuxiliaryLineList() {
        return auxiliaryLineList;
    }

    /**
     * 反转线段方向
     */
    public Line reverser() {
        if (invalid())
            return null;
        return new Line(up.copy(), down.copy());
    }

    /**
     * 获取与该条线段内缩进一段距离的内部线段
     *
     * @param sideIndentationDist 端点内缩进距离
     * @return 内缩进后的线段
     */
    public Line toIndentationLine(double sideIndentationDist) {
        if (sideIndentationDist < 0)
            return null;
        double length = getLength();
        if (length - 2 * sideIndentationDist <= 0) {
            return null;
        }
        ArrayList<Point> lepsList = PointList.getRotateLEPS(getAngle(), length - 2 * sideIndentationDist, getCenter());
        return new Line(lepsList.get(1), lepsList.get(0));
    }

    /**
     * 获取线段维持中心点的情况下，向外延伸的线段
     *
     * @param extendDist
     * @return 中心延长线段
     */
    public Line toExtendLine(double extendDist) {
        if (extendDist < 0)
            return null;
        ArrayList<Point> lepsList = PointList.getRotateLEPS(getAngle(), getLength() + 2 * extendDist, getCenter());
        return new Line(lepsList.get(1), lepsList.get(0));
    }

    /**
     * 复制线段
     */
    public Line copy() {
        if (invalid())
            return null;
        return new Line(down.copy(), up.copy());
    }

    /**
     * 求两线段交点
     *
     * @param line
     * @return
     */
    public Point getLineIntersectedPoint(Line line) {
        if (invalid() || line == null || line.invalid())
            return null;
        Point point = null;
        try {
            Point p1 = down;
            Point p2 = up;
            Point q1 = line.down;
            Point q2 = line.up;
            double tol = 0.00001d;
            double ua = (q2.x - q1.x) * (p1.y - q1.y) - (q2.y - q1.y) * (p1.x - q1.x);
            ua /= (q2.y - q1.y) * (p2.x - p1.x) - (q2.x - q1.x) * (p2.y - p1.y);
            double ub = (p2.x - p1.x) * (p1.y - q1.y) - (p2.y - p1.y) * (p1.x - q1.x);
            ub /= (q2.y - q1.y) * (p2.x - p1.x) - (q2.x - q1.x) * (p2.y - p1.y);
            if (ua > -tol && ua < 1 + tol && ub > -tol && ub < 1 + tol) {
                double atX = p1.x + ua * (p2.x - p1.x);
                double atY = p1.y + ua * (p2.y - p1.y);
                point = new Point(atX, atY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return point;
    }

    /**
     * 供外部调用获取两条线段的相交点
     *
     * @param L1
     * @param L2
     * @return
     */
    public static Point getLineIntersectedPoint(Line L1, Line L2) {
        if (L1 == null || L1.invalid() || L2 == null || L2.invalid())
            return null;
        return L1.getLineIntersectedPoint(L2);
    }

    /**
     * 将线段点放入列表中
     */
    public ArrayList<Point> toPointList() {
        if (invalid())
            return null;
        ArrayList<Point> pointsList = new ArrayList<>();
        pointsList.add(down.copy());
        pointsList.add(up.copy());
        return pointsList;
    }

    /**
     * 判断触摸点是否在线段上
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isTouchOnLine(double x, double y) {
        if (invalid())
            return false;
        Point point = new Point(x, y);
        ArrayList<Point> pointsList = PointList.getRotateVertexs(getAngle(), getThickess(), getLength(), getCenter());
        return PointList.pointRelationToPolygon(pointsList, point) != -1;
    }

    /**
     * 获取某个点与此线段是否是吸附关系，并返回吸附点
     *
     * @param x
     * @param y
     * @return
     */
    public Point getAdsorbPoint(double x, double y) {
        if (invalid())
            return null;
        Point point = new Point();
        point.x = x;
        point.y = y;
        double maxAdsorbLength = 2d * getThickess();
        ArrayList<Point> pointsList = PointList.getRotateVertexs(getAngle(), maxAdsorbLength, getLength(), getCenter());
        boolean invalid = PointList.pointRelationToPolygon(pointsList, point) == -1;
        if (invalid)
            return null;
        Point map = null;
        try {
            ArrayList<Point> lepsList = PointList.getRotateLEPS(getAngle() + 90d, 10d * maxAdsorbLength, point);
            Line lepsLine = new Line(lepsList.get(1), lepsList.get(0));
            map = getLineIntersectedPoint(lepsLine);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 获取某个点与此线段是否是吸附关系，并返回吸附点
     *
     * @param x
     * @param y
     * @param dist 相近的最大距离
     * @return
     */
    public Point getAdsorbPoint(double x, double y, double dist) {
        if (invalid())
            return null;
        Point point = new Point();
        point.x = x;
        point.y = y;
        double maxAdsorbLength = dist;
        ArrayList<Point> pointsList = PointList.getRotateVertexs(getAngle(), maxAdsorbLength, getLength(), getCenter());
        boolean invalid = PointList.pointRelationToPolygon(pointsList, point) == -1;
        if (invalid)
            return null;
        Point map = null;
        try {
            ArrayList<Point> lepsList = PointList.getRotateLEPS(getAngle() + 90d, 10 * maxAdsorbLength, point);
            Line lepsLine = new Line(lepsList.get(1), lepsList.get(0));
            map = getLineIntersectedPoint(lepsLine);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 判断线段是否共线
     *
     * @param line
     * @return
     */
    public boolean isCollineation(Line line) {
        if (line == null || line.invalid() || invalid())
            return false;
        // 角度相等或与反转方向的线段角度相等，即表示两线段平行
        Line reverserLine = line.reverser();
        double selfAngle = getAngle();
        if (line.getAngle() == selfAngle || reverserLine.getAngle() == selfAngle) {
            // 两线段有交点，即表示两线段共线
            return line.getLineIntersectedPoint(this) != null;
        }
        return false;
    }

    /**
     * 检测是否是此线段的端点
     *
     * @param point
     * @return 0为起点，1为终点，-1不是端点
     */
    public int isSidePoint(Point point) {
        if (point == null || invalid())
            return -1;
        boolean downEqual = down.equals(point);
        if (downEqual)
            return 0;
        boolean upEqual = up.equals(point);
        if (upEqual)
            return 1;
        return -1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(down, flags);
        dest.writeParcelable(up, flags);
        dest.writeDouble(thickess);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Line> CREATOR = new Creator<Line>() {
        @Override
        public Line createFromParcel(Parcel in) {
            return new Line(in);
        }

        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Line)) {
            return false;
        }
        Line other = (Line) obj;
        if (invalid() || other.invalid())
            return false;
        boolean matched = (down.equals(other.down) || down.equals(other.up)) && (up.equals(other.down) || up.equals(other.up));
        if (matched)
            return true;
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return thickess + "," + down + "," + up + "," + auxiliaryLineList;
    }

}
