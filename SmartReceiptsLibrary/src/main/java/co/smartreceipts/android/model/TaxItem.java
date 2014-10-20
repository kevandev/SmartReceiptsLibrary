package co.smartreceipts.android.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import android.text.TextUtils;
import android.util.Log;
import co.smartreceipts.android.BuildConfig;
import co.smartreceipts.android.persistence.Preferences;

public class TaxItem {
	
	private static final String TAG = "TaxItem";
	
	private static final int SCALE = 2;
	private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
	
	private BigDecimal mPercent;
	private BigDecimal mPrice, mTax;
	private boolean mUsePreTaxPrice;
	
	public TaxItem(String percent, Preferences preferences) {
		try {
			mPercent = new BigDecimal(percent);
		}
		catch (NumberFormatException e) {
			mPercent = null;
		}
		mUsePreTaxPrice = (preferences == null) ? true : preferences.getUsesPreTaxPrice();
	}
	
	public TaxItem(float percent, Preferences preferences) {
		mPercent = new BigDecimal(percent);
		mUsePreTaxPrice = (preferences == null) ? true : preferences.getUsesPreTaxPrice();
	}
	
	public TaxItem(BigDecimal percent, Preferences preferences) {
		mPercent = percent;
		mUsePreTaxPrice = (preferences == null) ? true : preferences.getUsesPreTaxPrice();
	}
	
	public TaxItem(BigDecimal percent, boolean usePreTaxPrice) {
		mPercent = percent;
		mUsePreTaxPrice = usePreTaxPrice;
	}
	
	public TaxItem(String percent, boolean usePreTaxPrice) {
		try {
			mPercent = new BigDecimal(percent);
		}
		catch (NumberFormatException e) {
			mPercent = null;
		}
		mUsePreTaxPrice = usePreTaxPrice;
	}
	
	public TaxItem(float percent, boolean usePreTaxPrice) {
		mPercent = new BigDecimal(percent);
		mUsePreTaxPrice = usePreTaxPrice;
	}
	
	public BigDecimal getPercent() {
		return mPercent;
	}
	
	public String getPercentAsString() {
		if (mPercent == null) {
			return "";
		}
		else {
			BigDecimal scaledPercent = mPercent.setScale(SCALE, ROUNDING_MODE);
			return getDecimalFormat().format(scaledPercent.doubleValue()) + "%";
		}
	}
	
	public void setPrice(String price) {
		if (TextUtils.isEmpty(price)) {
			mPrice = new BigDecimal(0);
			getTax();
		}
		try {
			mPrice = new BigDecimal(price.trim());
			getTax();
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, e.toString());
			mPrice = null;
		}
	}
	
	public boolean isValid() {
		return mTax != null;
	}
	
	public BigDecimal getTax() {
		if (mPercent == null || mPrice == null) {
			mTax = null;
		}
		else {
			Log.d(TAG, mPrice.toString());
			if (mUsePreTaxPrice) {
				mTax = mPrice.multiply(mPercent).divide(new BigDecimal(100), SCALE, ROUNDING_MODE);
			}
			else {
				mTax = mPrice.subtract(mPrice.divide(mPercent.divide(new BigDecimal(100), 10, ROUNDING_MODE).add(new BigDecimal(1)), SCALE, ROUNDING_MODE));
			}
		}
		return mTax;
	}
	
	@Override
	public String toString() {
		if (mTax == null) {
			return "";
		}
		else {
			return getDecimalFormat().format(mTax.doubleValue());
		}
	}
	
	private DecimalFormat getDecimalFormat() {
		DecimalFormat format = new DecimalFormat();
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(2);
		format.setGroupingUsed(false);
		return format;
	}

}