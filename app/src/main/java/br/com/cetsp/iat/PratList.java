package br.com.cetsp.iat;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import br.com.cetsp.iat.util.VehicleFix;

public class PratList extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iat iat = (Iat) getApplicationContext();
        if(!iat.isAuthenticated()){
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            return;
        }
        setContentView(R.layout.lista_prat);
        setListAdapter(new IatAdapter(this, iat.getReport()));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        

    }

    public void createReport(View view) {
        Intent intent = new Intent(this, CsiActivity.class);
        startActivityForResult(intent, 1);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Iat iat = (Iat) getApplicationContext();
        iat.append(data.getStringExtra("data"));
        Toast.makeText(this,"Mensagem Recebida ", Toast.LENGTH_LONG).show();
        setListAdapter(new IatAdapter(this, iat.getReport()));
    }

    private class IatAdapter extends ArrayAdapter<String> {
        private final ArrayList<String> items;

        public IatAdapter(Context c, ArrayList<String> j) {
            super(c, 0);
            items=j;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            try {
                JSONObject item= new JSONObject(items.get(position));
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_prat, parent, false);
                    byte[] decodedString = Base64.decode(item.getString("thumbnail"), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    decodedByte=Bitmap.createBitmap(decodedByte, 150,150,200, 200);
                    ((ImageView)convertView.findViewById(R.id.thumbnail)).setImageBitmap(decodedByte);
                    ArrayList<String> misshaps=new ArrayList<>();
                    JSONObject oa = item.optJSONObject("info");
                    if(oa!=null){
                        JSONArray a = oa.optJSONArray("vehicles");
                        if(a!=null){
                            for(int i=0;i<a.length();i++){
                                if(a.optJSONObject(i).optInt("model")==VehicleFix.COLISAO){
                                    misshaps.add(a.optJSONObject(i).optString("tipo_impacto"));
                                }
                            }
                        }
                    }
                    TextView t = (TextView) convertView.findViewById(R.id.textView_item);
                    t.setText(TextUtils.join(", ", misshaps));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return convertView;
        }
        public int getCount() {
            return items.size();
        }


    }
}
