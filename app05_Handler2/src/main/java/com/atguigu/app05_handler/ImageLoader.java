package com.atguigu.app05_handler;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * 动态加载图片并显示的工具类(使用三级缓存)
 * @author 张晓飞
 *
 */
/*
1). 根据url在一级缓存(Map)中取对应的Bitmap对象, 如果有, 显示
2). 如果没有, 根据url中图片文件从二级缓存中加载图片文件得到bitmap对象, 如果有, 显示, 保存一级缓存中
3). 如果没有, 显示提示正在加载的图片, 启动分线程(使用AsyncTask)根据url请求服务器获取Bitmap对象
	如果没有, 显示提示加载图片错误的图片
	如果有:
		1). 缓存到一级缓存
		2). 缓存到二级缓存
		3). 在主线程显示图片
 */
/*
 * 如何知道convertView被复用
 */
public class ImageLoader {

	private Context context;
	private int loadingIcon;
	private int errorIcon;
	private Map<String, Bitmap> cacheMap = new HashMap<String, Bitmap>();
	public ImageLoader(Context context, int loading, int error) {
		this.context = context;
		this.loadingIcon = loading;
		this.errorIcon = error;
	}

	/**
	 * 根据url加载图片并显示到指定的ImageView
	 * @param url
	 * @param imageView
	 */
	public void loadImage(final String url, final ImageView imageView) {
		
		//保存当前需要加载图片的url
		imageView.setTag(url);
		
		
		//1). 根据url在一级缓存(Map)中取对应的Bitmap对象, 如果有, 显示
		Bitmap bitmap = getBitmapFromFristCache(url);
		if(bitmap!=null) {
			imageView.setImageBitmap(bitmap);
			return;
		}
		
		//2). 如果没有, 根据url中图片文件从二级缓存中加载图片文件得到bitmap对象, 如果有, 显示, 保存一级缓存中
		bitmap = getBitmapFromSecondCache(url);
		if(bitmap!=null) {
			imageView.setImageBitmap(bitmap);
			cacheMap.put(url, bitmap);
			return;
		}
		
		/*
		3). 如果没有, 显示提示正在加载的图片, 启动分线程(使用AsyncTask)根据url请求服务器获取Bitmap对象
				如果没有, 显示提示加载图片错误的图片
				如果有:
					1). 缓存到一级缓存
					2). 缓存到二级缓存
					3). 在主线程显示图片
		 */
		
		//如果没有, 显示提示正在加载的图片
		imageView.setImageResource(loadingIcon);
		
		//启动分线程(使用AsyncTask)根据url请求服务器获取Bitmap对象
		new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}

			//分线程,请求获取bitmap
			@Override
			protected Bitmap doInBackground(Void... params) {
				
				Bitmap bitmap = null;
				
				//ImageView中保存的最新url
				String currentUrl = (String) imageView.getTag();
				if(url!=currentUrl) { //ImageView已经复用了, 没有必要请求获取图片
					return null;
				}
				
				
				try {
					//得到连接
					URL uRL = new URL(url);
					HttpURLConnection conn = (HttpURLConnection) uRL.openConnection();
					//设置
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);
					//连接
					conn.connect();
					//请求并得到数据并封闭为bitmap
					if(conn.getResponseCode()==200) {
						InputStream is = conn.getInputStream();
						//将is封装为bitmap
						bitmap = BitmapFactory.decodeStream(is);
						//关闭流
						is.close();
						
						/*
						 * 1). 缓存到一级缓存
					 	   2). 缓存到二级缓存
						 */
						if(bitmap!=null) {
							//1). 缓存到一级缓存
							cacheMap.put(url, bitmap);
							//2). 缓存到二级缓存
							// f10.jpg
							String fileName = url.substring(url.lastIndexOf("/")+1);
							// /storage/sdcard/Android/data/packageName/files/
							String filesPath = context.getExternalFilesDir(null).getAbsolutePath();
							String filePath = filesPath+"/"+fileName;
							bitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(filePath));
						}
					}
					//断开连接
					conn.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				
				return bitmap;
			}
			
			//显示图片
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				
				//得到ImageView中保存的最新url
				String currentUrl = (String) imageView.getTag();
				if(url!=currentUrl) { //ImageView已经复用了, 没有必要显示
					return;
				}
				
				if(bitmap==null) {//如果没有, 显示提示加载图片错误的图片
					imageView.setImageResource(errorIcon);
				} else {
					/*
					 3). 在主线程显示图片
					 */
					imageView.setImageBitmap(bitmap);
				}
			}
			
		}.execute();
	}

	/**
	 * 根据url在二级缓存中加载对应的文件返回bitmap
	 * @param url http://192.168.20.165:8080//L04_Web/images/f10.jpg
	 * @return
	 * /storage/sdcard/Android/data/packageName/files/f10.jpg
	 */
	private Bitmap getBitmapFromSecondCache(String url) {
		
		// f10.jpg
		String fileName = url.substring(url.lastIndexOf("/")+1);
		// /storage/sdcard/Android/data/packageName/files/
		String filesPath = context.getExternalFilesDir(null).getAbsolutePath();
		String filePath = filesPath+"/"+fileName;
		//加载图片文件返回bitmap
		return BitmapFactory.decodeFile(filePath);
	}

	/**
	 * 根据url在一级缓存中取对应的bitmap
	 * @param url
	 * @return
	 */
	private Bitmap getBitmapFromFristCache(String url) {
		return cacheMap.get(url);
	}

}
