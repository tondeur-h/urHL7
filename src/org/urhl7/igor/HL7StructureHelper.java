/*
 * The MIT License
 *
 * Copyright (c) 2011 David Morgan, University of Rochester Medical Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.urhl7.igor;

import java.util.*;
import org.urhl7.utils.*;
/**
 * Helper class that has methods that help move around the underlying data structure.
 * @author dmorgan
 */
public class HL7StructureHelper {
    private HL7Structure structure;

    private boolean NEVER_RETURN_NULL = true;
    private boolean ROLL_UP_NON_EXISTANT_DOT_ONE = true;

    public final static int SETTING_NEVER_RETURN_NULL = 1;
    public final static int SETTING_ROLL_UP_DOT_ONE = 2;

    private Map<HL7Location, DataField> _CACHE = null;
    //private LinkedList<HL7Location> _KEYCACHE = null;

    int cacheingDone = 0;

    /**
     * Create a HL7StructureHelper that is bound to the provided HL7Structure
     * @param structure the HL7Structure to bind to
     */
    public HL7StructureHelper(HL7Structure structure) {
        this.structure = structure;
        this._CACHE = new LinkedHashMap<HL7Location, DataField>();
        //this._KEYCACHE = new LinkedList<HL7Location>();
        refreshCache();
    }

    /**
     * Gets the current value of the setting defined by settingIdentifier
     * @param settingIdentifier the setting identifier to get
     * @return the value of the setting
     */
    public Object getSetting(int settingIdentifier) {
        switch (settingIdentifier) {
            case SETTING_NEVER_RETURN_NULL:
                return NEVER_RETURN_NULL;
            case SETTING_ROLL_UP_DOT_ONE:
                return ROLL_UP_NON_EXISTANT_DOT_ONE;
            default:
                throw new IllegalArgumentException(settingIdentifier + " is not a valid setting identifier.");
        }
    }

    /**
     * Sets the setting defined by settingIdentifier to value
     * @param settingIdentifier setting identifier (static variables of this class)
     * @param value numeric value to set this setting to
     */
    public void setSetting(int settingIdentifier, int value) {
        switch (settingIdentifier) {
            default:
                throw new IllegalArgumentException("Setting ID (" + settingIdentifier + ") is in invalid, or does not accept a integer parameter.");
        }
    }

    /**
     * Sets the setting defined by settingIdentifier to flag
     * @param settingIdentifier setting identifier (static variables of this class)
     * @param flag enable or disable the setting
     */
    public void setSetting(int settingIdentifier, boolean flag) {
        switch (settingIdentifier) {
            case SETTING_NEVER_RETURN_NULL:
                NEVER_RETURN_NULL = flag;
                break;
            case SETTING_ROLL_UP_DOT_ONE:
                ROLL_UP_NON_EXISTANT_DOT_ONE = flag;
                break;
            default:
                throw new IllegalArgumentException("Setting ID (" + settingIdentifier + ") is invalid or does not accept a boolean parameter.");
        }
    }

    /**
     * Determine if this structure has a particular data field or segment
     * @param descriptor string descriptor of the location of the data field or segment
     * @return if the data field or segment exists
     */
    public boolean has(String descriptor) {
        return has(HL7Location.parse(descriptor));
    }


    /**
     * Determine if this structure has a particular data field or segment
     * @param loc the HL7Location of the data field
     * @return if the data field or segment exists
     */
    public boolean has(HL7Location loc) { //this is a very... simple way to do it. could be more efficient.
        if (loc.hasSegment() && !loc.hasField()) {
            HL7Segment segment = getSegment(loc);
            if (segment != null) {
                return true;
            }
        } else {
            if (!(get(loc) instanceof EmptyField)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the first data field at a specified location. If the data field does not exist, rather than erroring,
     * it will return an EmptyField with no data.
     * @param descriptor String description of location
     * @return the first DataField that matches the descriptor
     */
    public DataField get(String descriptor) {
        return get(HL7Location.parse(descriptor), NEVER_RETURN_NULL);
    }

    /**
     * Retrieves the first data field at a specified location.
     * @param descriptor String description of location
     * @param neverReturnNull flag to return a null object on a not found object (false) or an EmptyField (true)
     * @return the first DataField that matches the descriptor
     */
    public DataField get(String descriptor, boolean neverReturnNull) {
        return get(HL7Location.parse(descriptor), neverReturnNull);
    }
    /**
     * Retrieves the first data field at a specified location. If the data field does not exist, rather than erroring,
     * it will return an EmptyField with no data.
     * @param loc the HL7Location of the data field
     * @return the first DataField that matches the descriptor
     */
    public DataField get(HL7Location loc) {
        return get(loc, NEVER_RETURN_NULL);
    }

    /**
     * Retrieves the first data field at a specified location.
     * @param loc the HL7Location of the data field
     * @param neverReturnNull flag to return a null object on a not found object (false) or an EmptyField (true)
     * @return the first DataField that matches the descriptor
     */
    public DataField get(HL7Location loc, boolean neverReturnNull) {
        try {
            if (loc.hasSegment() && loc.hasField()) {
                for(HL7Segment segment : getAllSegments(loc)) {
                    try {
                        List<HL7Field> fieldsThatMatch = new ArrayList<HL7Field>();
                        if (loc.isFieldIndexImplied()) {
                            fieldsThatMatch.addAll(segment.getRepeatingField(loc.getRepeatingFieldIndex()).getFields());
                        } else {
                            fieldsThatMatch.add(segment.getRepeatingField(loc.getRepeatingFieldIndex()).getField(loc.getFieldIndex()));
                        }


                        if(loc.hasSubcomponent()) {
                            for(HL7Field f : fieldsThatMatch) {
                                try {
                                    return f.getFieldComponent(loc.getComponentIndex()).getFieldSubcomponent(loc.getSubcomponentIndex());
                                } catch (Exception e) { /*e.printStackTrace();*/ }
                            }
                        } else if (loc.hasComponent()) {
                            for(HL7Field f : fieldsThatMatch) {
                                if (ROLL_UP_NON_EXISTANT_DOT_ONE && loc.getComponentHL7Position() == 1 && f.isBaseField()) {
                                    return f;
                                } else {
                                    try {
                                        return f.getFieldComponent(loc.getComponentIndex());
                                    } catch (Exception e) { /*e.printStackTrace();*/ }
                                }
                            }
                        } else {
                            try {
                                return fieldsThatMatch.get(0);
                            } catch (Exception e) { /*e.printStackTrace();*/ }
                        }
                    } catch (Exception e) { /*e.printStackTrace();*/ }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (neverReturnNull) {
            return new EmptyField();
        } else {
            return null;
        }
    }

    /**
     * Retrieves the all data fields matching a specified location, left to right, top to bottom. If the data field
     * does not exist, rather than erroring, it will return an empty list.
     * @param descriptor String description of location
     * @return all DataField object that match the descriptor, or an empty list if none do
     */
    public List<DataField> getAll(String descriptor) {
        return getAll(HL7Location.parse(descriptor));
    }

    /**
     * Retrieves the all data fields matching a specified location, left to right, top to bottom. If the data field
     * does not exist, rather than erroring, it will return an empty list.
     * @param loc the HL7Location of the data field
     * @return all DataField object that match the location, or an empty list if none do
     */
    public List<DataField> getAll(HL7Location loc) {
        ArrayList<DataField> retList = new ArrayList<DataField>();
        if (loc.hasSegment() && loc.hasField()) {
            for(HL7Segment segment : getAllSegments(loc)) {
                try {
                    List<HL7Field> fieldsThatMatch = new ArrayList<HL7Field>();
                    if (loc.isFieldIndexImplied()) {
                        //fieldsThatMatch.addAll(segment.getRepeatingField(loc.getRepeatingFieldIndex()).getFields());
                        for(HL7Field field : segment.getRepeatingField(loc.getRepeatingFieldIndex()).getFields()) {//
                            if (!fieldsThatMatch.contains(field)){
                                fieldsThatMatch.add(field);
                            }
                        }//
                    } else {
                        if (!fieldsThatMatch.contains(segment.getRepeatingField(loc.getRepeatingFieldIndex()).getField(loc.getFieldIndex()))) {//
                            fieldsThatMatch.add(segment.getRepeatingField(loc.getRepeatingFieldIndex()).getField(loc.getFieldIndex()));
                        }//
                    }

                    if(loc.hasSubcomponent()) {
                        for(HL7Field f : fieldsThatMatch) {
                            try {
                                if(!retList.contains(f.getFieldComponent(loc.getComponentIndex()).getFieldSubcomponent(loc.getSubcomponentIndex()))) {//
                                    retList.add(f.getFieldComponent(loc.getComponentIndex()).getFieldSubcomponent(loc.getSubcomponentIndex()));
                                }//
                            } catch (Exception e) { /*e.printStackTrace();*/ }
                        }
                    } else if (loc.hasComponent()) {
                        for(HL7Field f : fieldsThatMatch) {
                            if (ROLL_UP_NON_EXISTANT_DOT_ONE && loc.getComponentHL7Position() == 1 && f.isBaseField()) {
                                if (!retList.contains(f)) {//
                                    retList.add(f);
                                }//
                            } else {
                                try {
                                    if(!retList.contains(f.getFieldComponent(loc.getComponentIndex()))) {
                                        retList.add(f.getFieldComponent(loc.getComponentIndex()));
                                    }
                                } catch (Exception e) { /*e.printStackTrace();*/ }
                            }

                            try {
                                if (!retList.contains(f.getFieldComponent(loc.getComponentIndex()))) {
                                    retList.add(f.getFieldComponent(loc.getComponentIndex()));
                                }
                            } catch (Exception e) { /*e.printStackTrace();*/ }
                        }
                    } else {
                        try {
                            for(HL7Field fi : fieldsThatMatch) {
                                if(!retList.contains(fi)) {
                                    retList.add(fi);
                                }
                            }
                            //retList.addAll(fieldsThatMatch);
                            
                        } catch (Exception e) { /*e.printStackTrace();*/ }
                    }
                } catch (Exception e) { /*e.printStackTrace();*/ }
            }
        }
        return retList;
    }

    /**
     * Retrieves the first HL7Segment that matches the descriptor (top to bottom)
     * @param descriptor String description of location
     * @return the first HL7Segment that matches the descriptor
     */
    public HL7Segment getSegment(String descriptor) {
        //LocationSpecification ls = LocationParser.parse(descriptor);
        return getSegment(HL7Location.parse(descriptor));
    }

    /**
     * Retrieves the first HL7Segment that matches the HL7Location (top to bottom)
     * @param loc the HL7Location of the segment
     * @return the first HL7Segment that matches the LocationSpecification
     */
    public HL7Segment getSegment(HL7Location loc) {
        int positionCount = -1;
        for(HL7Segment segment : structure.getSegments()) {
            if (segment.getSegmentName().equalsIgnoreCase(loc.getSegmentName())) {
                positionCount++;
                if(loc.getSegmentIndex() == positionCount) {
                    return segment;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all segments that match the descriptor (from top to bottom)
     * @param descriptor String description of location
     * @return all HL7Segments that match the descriptor
     */
    public List<HL7Segment> getAllSegments(String descriptor) {
        //LocationSpecification ls = LocationParser.parse(descriptor);
        return getAllSegments(HL7Location.parse(descriptor));
    }

    /**
     * Retrieves all segments that match the HL7Location (from top to bottom)
     * @param loc the HL7Location of the segments
     * @return all HL7Segments that match the specification
     */
    public List<HL7Segment> getAllSegments(HL7Location loc) {
        ArrayList<HL7Segment> segments = new ArrayList<HL7Segment>();
        int positionCount = -1;
        for(HL7Segment segment : structure.getSegments()) {
            if (segment.getSegmentName().equalsIgnoreCase(loc.getSegmentName())) {
                positionCount++;
                if( loc.isSegmentIndexImplied() ) {
                    segments.add(segment);
                } else {
                    if(loc.getSegmentIndex() == positionCount) {
                        segments.add(segment);
                    }
                }
            }
        }
        return segments;
    }


    public DataField nGet(String descriptor) {
        return nGet(HL7Location.parse(descriptor));
    }

    public DataField nGet(HL7Location loc) {
        if (structure.needsRecache) {
            refreshCache();
        }

        for (Map.Entry<HL7Location, DataField> entry : _CACHE.entrySet()) {
            if (entry.getKey().matches(loc)) {
                return entry.getValue();
            }
        }


        return new EmptyField();
    }

    public List<DataField> nGetAll(HL7Location loc) {
        if (structure.needsRecache) {
            refreshCache();
        }
        ArrayList<DataField> listOfFields = new ArrayList<DataField>();

        for (Map.Entry<HL7Location, DataField> entry : _CACHE.entrySet()) {
            if (entry.getKey().matches(loc)) {
                listOfFields.add(entry.getValue());
            }
        }
        
        return listOfFields;
    }




    /**
     * This method is magic, very buggy, but also very useful. Use with care.
     * @param descriptor
     * @return
     */
    /*public List<List<DataField>> getRelated(String descriptor) {
        String[] eles = StringHelper.explode(descriptor, ",");
        List<HL7Location> locs = new ArrayList<HL7Location>();
        for(String ele : eles) {
            locs.add(HL7Location.parse(ele.trim()));
        }
        return getRelated(locs);
    }*/

    /**
     * This method is magic, very buggy, but also very useful. Use with care.
     * @param locs
     * @return
     */
    /*public List<List<DataField>> getRelated(List<HL7Location> locs) {
        ArrayList<HL7Location> bucketHeader = new ArrayList<HL7Location>();
        for(HL7Location loc : locs) {
            bucketHeader.add(HL7Location.parse(loc.getShortHL7Location()));
        }

        System.out.println(bucketHeader);

        Iterator<Map.Entry<HL7Location, DataField>> it = _CACHE.entrySet().iterator();

        Map<HL7Location, DataField> lastData = new HashMap<HL7Location, DataField>();

        List<Map<HL7Location, DataField>> dset = new ArrayList<Map<HL7Location, DataField>>();
        while(it.hasNext()) {
            Map.Entry<HL7Location, DataField> entry = it.next();
            for(int i=0; i<bucketHeader.size(); i++) {
                if (entry.getKey().matches(bucketHeader.get(i))) {
                    //System.out.println(bucketHeader.get(i).getHL7Location() + " / " + entry.getKey().getHL7Location() + " : " + entry.getValue());
                    lastData.put(bucketHeader.get(i), entry.getValue());
                    if (i == (bucketHeader.size()-1)) {
                        //System.out.println(lastData);
                        //bucket.put(lastData);
                        Map<HL7Location, DataField> newRecord = new HashMap<HL7Location, DataField>();
                        newRecord.putAll(lastData);
                        dset.add(newRecord);
                    }
                }
            }
        }


        //System.out.println(dset);

        for(Map<HL7Location, DataField> record : dset) {
            //System.out.println(record);
            System.out.println(record.get(HL7Location.parse("PID-3")) + " obr: " + record.get(HL7Location.parse("OBR-2")) + " with " + record.get(HL7Location.parse("OBX-5")));
        }


        return null;
    }*/

    private void refreshCache() {
        cacheingDone++;
        LinkedHashMap<HL7Location, DataField> table = new LinkedHashMap<HL7Location, DataField>();

        //for(HL7Segment segment : structure.getSegments()) {
        List<HL7Segment> segmentList = structure.getSegments();
        HashMap<String, Integer> mappingIndex = new HashMap<String,Integer>();

        for(int sIdx=0; sIdx<segmentList.size(); sIdx++) {
            HL7Segment segment = segmentList.get(sIdx);
            String segmentName = segment.getSegmentName();
            Integer segmentIndex = mappingIndex.get(segmentName);
            if (segmentIndex == null) {
                mappingIndex.put(segmentName, 0);
                segmentIndex = 0;
            } else {
                segmentIndex++;
                mappingIndex.put(segmentName, segmentIndex);
            }


            List<HL7RepeatingField> repeatingFieldList = segment.getRepeatingFields();
            for(int rfIdx=0; rfIdx<repeatingFieldList.size(); rfIdx++) {
                HL7RepeatingField rf = repeatingFieldList.get(rfIdx);
                    List<HL7Field> fieldList = rf.getFields();
                    for(int fIdx=0; fIdx<fieldList.size(); fIdx++){
                        HL7Field field = fieldList.get(fIdx);

                        if (field.isBaseField() ){
                            HL7Location loc = new HL7Location(segmentName, segmentIndex, rfIdx, fIdx, -1, -1);
                            table.put(loc, field);
                        } else {
                            List<HL7FieldComponent> fieldCompList = field.getFieldComponents();
                            for(int fcIdx=0; fcIdx<fieldCompList.size(); fcIdx++) {
                                HL7FieldComponent fieldcomp = fieldCompList.get(fcIdx);
                                if(fieldcomp.isBaseField() ){
                                    HL7Location loc = new HL7Location(segmentName, segmentIndex, rfIdx, fIdx, fcIdx, -1);
                                    table.put(loc, fieldcomp);
                                } else {
                                    List<HL7FieldSubcomponent> fieldSubcompList = fieldcomp.getFieldSubcomponents();
                                    for(int fscIdx=0; fscIdx<fieldSubcompList.size(); fscIdx++) {
                                        HL7FieldSubcomponent fieldsub = fieldSubcompList.get(fscIdx);
                                        if(fieldsub.isBaseField() ){
                                            HL7Location loc = new HL7Location(segmentName, segmentIndex, rfIdx, fIdx, fcIdx, fscIdx);
                                            table.put(loc, fieldsub);
                                        }
                                    }
                                }
                            }
                        }
                    }
                //}
            }
        }

        _CACHE = table;
        structure.needsRecache = false;
    }

    public void printCacheUsage() {
        //System.out.println("Cache run: " + cacheingDone);
    }

}
