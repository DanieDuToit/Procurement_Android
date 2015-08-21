package za.co.proteacoin.procurementandroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by dutoitd1 on 2015/08/18.
 */
public class CompanyInit_Activity extends Activity {
    private GlobalState gs;
    private int numberrOfSecurityIDViews;
    private SharedPreferences sharedPref;
    private EditText etSAPDeviceId;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.companyinitialization);

        gs = (GlobalState) getApplication();

        etSAPDeviceId = (EditText) findViewById(R.id.etDeviceId);
        btnSubmit = (Button) findViewById(R.id.btnSubmitCompanyInfo);
        TextView tv = (TextView) findViewById(R.id.tvAviKey);
        tv.setText(gs.getIvKey());

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etSAPDeviceId.getText().toString().equals("")) {
                    gs.showAlertDialog(CompanyInit_Activity.this, "Device Id", "You must supply the Company's Device Id", false);
                } else {
                    try {
                        gs.setCalmDeviceId(Integer.parseInt(etSAPDeviceId.getText().toString()));
                    } catch (NumberFormatException e) {
                        gs.showAlertDialog(CompanyInit_Activity.this, "Device Id", "The Device Id can only be numeric", false);
                        return;
                    }
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("sapDeviceId", gs.getCalmDeviceId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }
}
