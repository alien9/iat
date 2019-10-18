package br.com.cetsp.iat.ftt;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import br.com.cetsp.iat.R;
import okhttp3.OkHttpClient;

/**
 * Created by mgaldieri on 8/18/16.
 */
public class FormControl {
    private static OkHttpClient client;

    public static void freeze(View v){
        if(v==null)return;
        if(v instanceof Spinner){}
    }

    public static OkHttpClient getHttpClient(Context context) {
        if(client!=null) return client;
        try {
            java.security.KeyStore trusted = java.security.KeyStore.getInstance("BKS");
            java.io.InputStream in = context.getResources().openRawResource(R.raw.ftt_bks);
            trusted.load(in, "ftt_tcasmt2mlb996584".toCharArray());
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            javax.net.ssl.TrustManagerFactory trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trusted);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            SSLSocketFactory sf = sslContext.getSocketFactory();
            TrustEveryoneManager tau = new TrustEveryoneManager();

            client = new OkHttpClient.Builder()
            //        .sslSocketFactory(sf,tau)
                    .build();

        } catch (IOException |NoSuchAlgorithmException |CertificateException |KeyStoreException |KeyManagementException e) {
            e.printStackTrace();
            Log.e("E-BI SPTRANS", String.format("Erro ao carregar certificado: %s", (Object[]) new String[]{e.getMessage()}));
            Toast.makeText(context, "Certificado de segurança inválido. Por favor entrar em contato com a regional", Toast.LENGTH_LONG).show();
        }
        return client;
    }


    static class TrustEveryoneManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] arg0, String arg1){}
        public void checkServerTrusted(X509Certificate[] arg0, String arg1){}
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
