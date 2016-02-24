package com.atguigu.app05_handler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends Activity {

	protected static final int WHAT_GET_INFOS_SUCCESS = 1;
	protected static final int WHAT_GET_INFOS_ERROR = 2;
	private ListView lv_main;
	private LinearLayout ll_main_loading;
	private List<ShopInfo> data = new ArrayList<ShopInfo>();
	private ShopInfoAdapter adapter;
	private ProgressBar pb_foot_loading;
	private TextView tv_foot_loading;

	private boolean isLoading = false; //标识是否正在加载中

	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case WHAT_GET_INFOS_SUCCESS:
				List<ShopInfo> list = (List<ShopInfo>) msg.obj;

				//初始化显示列表
				if (data.size() == 0) {
					data = list;
					lv_main.setAdapter(adapter);
					ll_main_loading.setVisibility(View.GONE);
				} else {//更新列表
					data.addAll(list);
					adapter.notifyDataSetChanged();
				}

				//判断是否还有更多数据
				if (list.size() < PAGE_SIZE) {
					pb_foot_loading.setVisibility(View.GONE);
					tv_foot_loading.setText("已经显示完所有数据");
				}

				isLoading = false;
				break;
			case WHAT_GET_INFOS_ERROR:
				//3.隐藏提示视图, 提示请求失败
				ll_main_loading.setVisibility(View.GONE);
				Toast.makeText(MainActivity.this, "请求失败!", 0).show();
				break;
			default:
				break;
			}
		}
	};
	private AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE //停止不动
					&& lv_main.getLastVisiblePosition()==data.size()//loading可见
					&& pb_foot_loading.isShown() //还有更多的数据
					&& !isLoading) {  //不是正在加载
				loadData();
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		lv_main = (ListView) findViewById(R.id.lv_main);
		ll_main_loading = (LinearLayout) findViewById(R.id.ll_main_loading);
		adapter = new ShopInfoAdapter();
		//异步请求网络得到列表数据并并显示
			//1. 主线程,显示提示视图
		ll_main_loading.setVisibility(View.VISIBLE);
			//2.分线程,请求服务器得到列表数据
		loadData();

		//给listView添加foot
		addFoot();

		//给ListView设置滚动的监听
		lv_main.setOnScrollListener(onScrollListener);
	}

	private void addFoot() {
		View footView = View.inflate(this, R.layout.foot_view, null);
		pb_foot_loading = (ProgressBar) footView.findViewById(R.id.pb_foot_loading);
		tv_foot_loading = (TextView) footView.findViewById(R.id.tv_foot_loading);
		lv_main.addFooterView(footView);
	}

	public static final int PAGE_SIZE = 8;

	/**
	 * 加载数据显示
	 */
	private void loadData() {

		isLoading = true;

		new Thread(){
			public void run() {
				try {
					//String path = "http://192.168.20.165:8080/L04_Web/ShopInfoListServlet";
					int page = data.size()/PAGE_SIZE;
					Log.e("TAG", "page="+page);
					String path = "http://192.168.10.165:8080/L04_Web/ShopInfoPageListServlet?page="
							+page+"&size="+PAGE_SIZE;
					//得到连接
					URL url = new URL(path);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					//设置
					connection.setConnectTimeout(5000);
					connection.setReadTimeout(5000);
					//连接
					connection.connect();
					//请求并读取服务器返回的数据
					int code = connection.getResponseCode();
					if (code == 200) {
						InputStream is = connection.getInputStream();
						//将is读取成一个String
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int len = -1;
						while ((len = is.read(buffer)) != -1) {
							baos.write(buffer, 0, len);
						}

						baos.close();
						is.close();

						String result = baos.toString(); //json格式的字符串

						//解析json字符串成List<ShopInfo>
						List<ShopInfo> list = new Gson().fromJson(result, new TypeToken<List<ShopInfo>>() {
						}.getType());

						//通知主线程显示
						Message message = Message.obtain();
						message.what = WHAT_GET_INFOS_SUCCESS;
						message.obj = list;
						handler.sendMessage(message);
					}

				} catch (Exception e) {
					//e.printStackTrace();
					//发送请求失败的消息
					handler.sendEmptyMessage(WHAT_GET_INFOS_ERROR);
				}

			}
		}.start();
	}

	class ShopInfoAdapter extends BaseAdapter {

		private ImageLoader imageLoader;
		
		public ShopInfoAdapter() {
			imageLoader = new ImageLoader(MainActivity.this, R.drawable.loading, R.drawable.error);
		}
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			if(convertView==null) {
				convertView = View.inflate(MainActivity.this, R.layout.item_main, null);
			}
			
			//得到数据对象
			ShopInfo shopInfo = data.get(position);
			
			//得到子View
			ImageView imageView = (ImageView) convertView.findViewById(R.id.iv_item_icon);
			TextView nameTV = (TextView) convertView.findViewById(R.id.tv_item_name);
			TextView priceTV = (TextView) convertView.findViewById(R.id.tv_item_price);
			
			//设置数据
			nameTV.setText(shopInfo.getName());
			priceTV.setText("人民币:"+shopInfo.getPrice()+"元");
			//动态显示图片
			String url = shopInfo.getImagePath();
			//加载图片
			imageLoader.loadImage(url, imageView);
			
			return convertView;
		}
	}
}
