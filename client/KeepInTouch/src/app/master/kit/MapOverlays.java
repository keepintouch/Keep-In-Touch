package app.master.kit;

import java.util.ArrayList;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MapOverlays extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();

	public MapOverlays(Drawable marker) {
		super(boundCenterBottom(marker));
		// TODO Auto-generated constructor stub

		populate();
	}

	public void addItem(GeoPoint p, String title, String snippet){
		OverlayItem newItem = new OverlayItem(p, title, snippet);
		overlayItemList.add(newItem);
		populate();
	}

	public void clearItems(){
		overlayItemList.clear();
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return overlayItemList.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return overlayItemList.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// TODO Auto-generated method stub
		super.draw(canvas, mapView, shadow);
		//boundCenterBottom(marker);
	}

	@Override
	protected boolean onTap(int index) {
		try {
			OverlayItem item = overlayItemList.get(index);
			Toast.makeText(LocationMapActivity.context, item.getTitle()+"\n"+item.getSnippet(), Toast.LENGTH_LONG).show();
		} catch (ArrayIndexOutOfBoundsException e) {
			//pass
		}
		return true;
	}
}