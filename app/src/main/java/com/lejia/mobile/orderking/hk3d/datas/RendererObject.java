package com.lejia.mobile.orderking.hk3d.datas;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import com.lejia.mobile.orderking.hk3d.ViewingShader;
import com.lejia.mobile.orderking.hk3d.classes.LJ3DPoint;
import com.lejia.mobile.orderking.hk3d.classes.Texture;
import com.lejia.mobile.orderking.utils.TextUtils;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Author by HEKE
 *
 * @time 2018/7/16 10:32
 * TODO: 渲染对象父类
 */
public abstract class RendererObject {

    public float[] vertexs; // 顶点
    public FloatBuffer vertexsBuffer; // 顶点字节缓存

    public float[] texcoord; // 纹理uv
    public FloatBuffer texcoordBuffer; // 纹理uv字节缓存

    public float[] normalTexcoord; // 法线纹理uv
    public FloatBuffer normalTexcoordBuffer; // 法线纹理uv字节缓存

    public int[] indices; // 索引

    public float[] normals; // 法线
    public FloatBuffer normalsBuffer; // 法线字节缓存

    public int textureId = -1; // 纹理对应编号

    public int normalTextureId = -1; // 法线纹理对应编号

    public String textureName; // 纹理名称

    public String normalTextureName; // 法线纹理名称

    public ArrayList<LJ3DPoint> lj3DPointsList; // 三维围点列表

    /**
     * 基础渲染
     *
     * @param positionAttribute 位置编号
     * @param normalAttribute   法线编号
     * @param colorAttribute    颜色编号
     * @param onlyPosition      是否阴影仅顶点着色
     */
    public abstract void render(int positionAttribute, int normalAttribute, int colorAttribute, boolean onlyPosition);

    /**
     * 创建材质纹理对应编号及缓存
     *
     * @param key    唯一身份证
     * @param bitmap 对应贴图
     * @return 返回材质纹理对应渲染编号
     */
    public int createTextureIdAndCache(String key, Bitmap bitmap) {
        if (TextUtils.isTextEmpity(key) || bitmap == null || bitmap.isRecycled())
            return -1;
        int[] textureId = new int[1];
        try {
            // 判断是否包含缓存
            Texture texture = TexturesCache.get(key);
            if (texture != null) {
                textureId[0] = texture.textureId;
            } else {
                GLES30.glGenTextures(1, textureId, 0);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId[0]);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
                // 存入缓存
                TexturesCache.put(key, textureId[0], bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textureId[0];
    }

    /**
     * 释放数据
     */
    public void release() {
        GLES30.glDeleteTextures(2, new int[]{textureId, normalTextureId}, 0);
        GLES30.glDeleteProgram(ViewingShader.mProgram);
    }

}