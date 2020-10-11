package com.android.server.wifi.util;

public class KalmanFilter {

    /* renamed from: mF */
    public Matrix f35mF;

    /* renamed from: mH */
    public Matrix f36mH;

    /* renamed from: mP */
    public Matrix f37mP;

    /* renamed from: mQ */
    public Matrix f38mQ;

    /* renamed from: mR */
    public Matrix f39mR;

    /* renamed from: mx */
    public Matrix f40mx;

    public void predict() {
        this.f40mx = this.f35mF.dot(this.f40mx);
        this.f37mP = this.f35mF.dot(this.f37mP).dotTranspose(this.f35mF).plus(this.f38mQ);
    }

    public void update(Matrix z) {
        Matrix y = z.minus(this.f36mH.dot(this.f40mx));
        Matrix tK = this.f37mP.dotTranspose(this.f36mH).dot(this.f36mH.dot(this.f37mP).dotTranspose(this.f36mH).plus(this.f39mR).inverse());
        this.f40mx = this.f40mx.plus(tK.dot(y));
        this.f37mP = this.f37mP.minus(tK.dot(this.f36mH).dot(this.f37mP));
    }

    public String toString() {
        return "{F: " + this.f35mF + " Q: " + this.f38mQ + " H: " + this.f36mH + " R: " + this.f39mR + " P: " + this.f37mP + " x: " + this.f40mx + "}";
    }
}
