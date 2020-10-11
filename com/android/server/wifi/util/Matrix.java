package com.android.server.wifi.util;

public class Matrix {

    /* renamed from: m */
    public final int f41m;
    public final double[] mem;

    /* renamed from: n */
    public final int f42n;

    public Matrix(int rows, int cols) {
        this.f42n = rows;
        this.f41m = cols;
        this.mem = new double[(rows * cols)];
    }

    public Matrix(int stride, double[] values) {
        this.f42n = ((values.length + stride) - 1) / stride;
        this.f41m = stride;
        this.mem = values;
        if (this.mem.length != this.f42n * this.f41m) {
            throw new IllegalArgumentException();
        }
    }

    public Matrix(Matrix that) {
        this.f42n = that.f42n;
        this.f41m = that.f41m;
        this.mem = new double[that.mem.length];
        int i = 0;
        while (true) {
            double[] dArr = this.mem;
            if (i < dArr.length) {
                dArr[i] = that.mem[i];
                i++;
            } else {
                return;
            }
        }
    }

    public double get(int i, int j) {
        int i2;
        if (i >= 0 && i < this.f42n && j >= 0 && j < (i2 = this.f41m)) {
            return this.mem[(i2 * i) + j];
        }
        throw new IndexOutOfBoundsException();
    }

    public void put(int i, int j, double v) {
        int i2;
        if (i < 0 || i >= this.f42n || j < 0 || j >= (i2 = this.f41m)) {
            throw new IndexOutOfBoundsException();
        }
        this.mem[(i2 * i) + j] = v;
    }

    public Matrix plus(Matrix that) {
        return plus(that, new Matrix(this.f42n, this.f41m));
    }

    public Matrix plus(Matrix that, Matrix result) {
        int i;
        int i2 = this.f42n;
        if (i2 == that.f42n && (i = this.f41m) == that.f41m && i2 == result.f42n && i == result.f41m) {
            int i3 = 0;
            while (true) {
                double[] dArr = this.mem;
                if (i3 >= dArr.length) {
                    return result;
                }
                result.mem[i3] = dArr[i3] + that.mem[i3];
                i3++;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Matrix minus(Matrix that) {
        return minus(that, new Matrix(this.f42n, this.f41m));
    }

    public Matrix minus(Matrix that, Matrix result) {
        int i;
        int i2 = this.f42n;
        if (i2 == that.f42n && (i = this.f41m) == that.f41m && i2 == result.f42n && i == result.f41m) {
            int i3 = 0;
            while (true) {
                double[] dArr = this.mem;
                if (i3 >= dArr.length) {
                    return result;
                }
                result.mem[i3] = dArr[i3] - that.mem[i3];
                i3++;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Matrix times(double scalar) {
        return times(scalar, new Matrix(this.f42n, this.f41m));
    }

    public Matrix times(double scalar, Matrix result) {
        if (this.f42n == result.f42n && this.f41m == result.f41m) {
            int i = 0;
            while (true) {
                double[] dArr = this.mem;
                if (i >= dArr.length) {
                    return result;
                }
                result.mem[i] = dArr[i] * scalar;
                i++;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Matrix dot(Matrix that) {
        return dot(that, new Matrix(this.f42n, that.f41m));
    }

    public Matrix dot(Matrix that, Matrix result) {
        if (this.f42n == result.f42n && this.f41m == that.f42n && that.f41m == result.f41m) {
            for (int i = 0; i < this.f42n; i++) {
                for (int j = 0; j < that.f41m; j++) {
                    double s = 0.0d;
                    for (int k = 0; k < this.f41m; k++) {
                        s += get(i, k) * that.get(k, j);
                    }
                    result.put(i, j, s);
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix transpose() {
        return transpose(new Matrix(this.f41m, this.f42n));
    }

    public Matrix transpose(Matrix result) {
        if (this.f42n == result.f41m && this.f41m == result.f42n) {
            for (int i = 0; i < this.f42n; i++) {
                for (int j = 0; j < this.f41m; j++) {
                    result.put(j, i, get(i, j));
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix inverse() {
        return inverse(new Matrix(this.f42n, this.f41m), new Matrix(this.f42n, this.f41m * 2));
    }

    public Matrix inverse(Matrix result, Matrix scratch) {
        Matrix matrix = result;
        Matrix matrix2 = scratch;
        int i = this.f42n;
        int i2 = this.f41m;
        if (i == i2 && i == matrix.f42n && i2 == matrix.f41m && i == matrix2.f42n && i2 * 2 == matrix2.f41m) {
            int i3 = 0;
            while (i3 < this.f42n) {
                int j = 0;
                while (j < this.f41m) {
                    matrix2.put(i3, j, get(i3, j));
                    matrix2.put(i3, this.f41m + j, i3 == j ? 1.0d : 0.0d);
                    j++;
                }
                i3++;
            }
            int i4 = 0;
            while (true) {
                int ibest = this.f42n;
                if (i4 < ibest) {
                    int ibest2 = i4;
                    double vbest = Math.abs(matrix2.get(ibest2, ibest2));
                    for (int ii = i4 + 1; ii < this.f42n; ii++) {
                        double v = Math.abs(matrix2.get(ii, i4));
                        if (v > vbest) {
                            ibest2 = ii;
                            vbest = v;
                        }
                    }
                    if (ibest2 != i4) {
                        for (int j2 = 0; j2 < matrix2.f41m; j2++) {
                            double t = matrix2.get(i4, j2);
                            matrix2.put(i4, j2, matrix2.get(ibest2, j2));
                            matrix2.put(ibest2, j2, t);
                        }
                    }
                    double d = matrix2.get(i4, i4);
                    if (d != 0.0d) {
                        for (int j3 = 0; j3 < matrix2.f41m; j3++) {
                            matrix2.put(i4, j3, matrix2.get(i4, j3) / d);
                        }
                        for (int ii2 = i4 + 1; ii2 < this.f42n; ii2++) {
                            double d2 = matrix2.get(ii2, i4);
                            for (int j4 = 0; j4 < matrix2.f41m; j4++) {
                                matrix2.put(ii2, j4, matrix2.get(ii2, j4) - (matrix2.get(i4, j4) * d2));
                            }
                        }
                        i4++;
                    } else {
                        throw new ArithmeticException("Singular matrix");
                    }
                } else {
                    for (int i5 = ibest - 1; i5 >= 0; i5--) {
                        for (int ii3 = 0; ii3 < i5; ii3++) {
                            double d3 = matrix2.get(ii3, i5);
                            for (int j5 = 0; j5 < matrix2.f41m; j5++) {
                                matrix2.put(ii3, j5, matrix2.get(ii3, j5) - (matrix2.get(i5, j5) * d3));
                            }
                        }
                    }
                    for (int i6 = 0; i6 < matrix.f42n; i6++) {
                        for (int j6 = 0; j6 < matrix.f41m; j6++) {
                            matrix.put(i6, j6, matrix2.get(i6, this.f41m + j6));
                        }
                    }
                    return matrix;
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Matrix dotTranspose(Matrix that) {
        return dotTranspose(that, new Matrix(this.f42n, that.f42n));
    }

    public Matrix dotTranspose(Matrix that, Matrix result) {
        if (this.f42n == result.f42n && this.f41m == that.f41m && that.f42n == result.f41m) {
            for (int i = 0; i < this.f42n; i++) {
                for (int j = 0; j < that.f42n; j++) {
                    double s = 0.0d;
                    for (int k = 0; k < this.f41m; k++) {
                        s += get(i, k) * that.get(j, k);
                    }
                    result.put(i, j, s);
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Matrix)) {
            return false;
        }
        Matrix other = (Matrix) that;
        if (this.f42n != other.f42n || this.f41m != other.f41m) {
            return false;
        }
        int i = 0;
        while (true) {
            double[] dArr = this.mem;
            if (i >= dArr.length) {
                return true;
            }
            if (dArr[i] != other.mem[i]) {
                return false;
            }
            i++;
        }
    }

    public int hashCode() {
        int h = (this.f42n * 101) + this.f41m;
        int i = 0;
        while (true) {
            double[] dArr = this.mem;
            if (i >= dArr.length) {
                return h;
            }
            h = (h * 37) + Double.hashCode(dArr[i]);
            i++;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.f42n * this.f41m * 8);
        sb.append("[");
        for (int i = 0; i < this.mem.length; i++) {
            if (i > 0) {
                sb.append(i % this.f41m == 0 ? "; " : ", ");
            }
            sb.append(this.mem[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
