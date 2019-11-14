package erenik.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import erenik.evergreen.common.combat.Combatable;
import erenik.evergreen.common.logging.Log;

/**
 * Implemented as an arrayList in the background, but uses simple arrays for serializable transfer (instead of using the collections which fail during transmission or saving/loading sometimes).
 * Created by Emil on 2017-03-25.
 */
public class EList<cls> implements Serializable {
    private static final long serialVersionUID = 1L;

    ArrayList<cls> arrL = new ArrayList<>();

    public EList() {
    }

    public EList(cls[] values) {
        addAll(values);
    }

    public <otherClass> EList(EList<otherClass> compatibleList) {
        addAllCompatible(compatibleList);
    }

    public void add(cls obj) {
        arrL.add(obj);
    }

    private int E_SERIALIZABLE = 0,
            SERIALIZABLE = 1,
            NOT_SERIALIZABLE = 10;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(arrL.toArray());
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        readFrom(in);
    }

    private void readObjectNoData() throws ObjectStreamException
    {}

    public boolean readFrom(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            arrL = new ArrayList<>();
            cls[] obj = (cls[]) in.readObject();
            addAll(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Requires objects to implement ESerializable writeTo/readFrom fucntions.
    public boolean writeTo(ObjectOutputStream out) throws IOException {
        out.writeInt(arrL.size());
        for (int i = 0; i < arrL.size(); ++i) {
            ESerializable es = (ESerializable) arrL.get(i);
            es.writeTo(out);
        }
        return true;
    }

    public EList clone(){
        EList<cls> newL = new EList<>();
        for (int i = 0; i < arrL.size(); ++i)
            newL.add(arrL.get(i));
        return newL;
    }

    /*
    public <Tc extends ESerializable> void readFrom(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int num = in.readInt();
        for (int i = 0; i < num; ++i) {
            Tc c = Tc.Construct();
            ESerializable es = c;
            ESerializable obj = es.Construct();
            obj.readFrom(in);
        }
    }*/

    private void addAll(cls[] values) {
        for (int i = 0; i < values.length; ++i)
            arrL.add(values[i]);
    }
    private <otherClass> void addAllCompatible(EList<otherClass> compatibleList) {
        for (int i = 0; i < compatibleList.size(); ++i)
            arrL.add((cls) compatibleList.get(i));
    }


    public int size() {
        return arrL.size();
    }

    public cls get(int i) {
        return arrL.get(i);
    }

    public void clear() {
        arrL.clear();;
    }

    public ArrayList<cls> asArrayList() {
        return arrL;
    }

    public void remove(cls object) {
        arrL.remove(object);
    }

    public void remove(int index) {
        arrL.remove(index);
    }

    public int indexOf(cls object) {
        return arrL.indexOf(object);
    }

    public void addAll(EList<cls> objs) {
        arrL.addAll(objs.asArrayList());
    }

    public boolean contains(cls object) {
        return arrL.contains(object);
    }

    public EList<cls> subList(int startIndex, int endIndexInclusive) {
        if (arrL.size() == 0)
            return new EList<>();
        if (startIndex < 0)
            startIndex = 0;
        EList<cls> ell = new EList<>();
        for (int i = startIndex; i <= endIndexInclusive && i < arrL.size(); ++i){
            ell.add(arrL.get(i));
        }
        return ell;
    }

    public EList<cls> ReverseOrder() {
        EList<cls> newList = new EList<>();
        for (int i = arrL.size() - 1; i >= 0; --i){
            newList.add(arrL.get(i));
        }
        return newList;
    }

    public EList<cls> subList(int startIndex) {
        return subList(startIndex, arrL.size());
    }

    public void swap(int index1, int index2) {
        Collections.swap(arrL, index1, index2);
    }
}
