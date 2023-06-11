package KasperCommons.Parser;

import KasperCommons.DataStructures.KasperList;
import KasperCommons.DataStructures.KasperMap;
import KasperCommons.DataStructures.KasperObject;
import KasperCommons.DataStructures.KasperString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class KasperConstructor {
    private Node args;
    private Node base_value;
    private Document document;

    public KasperConstructor(KasperDocument kasperDocument){
        this.document = kasperDocument.document;
        args = document.getElementsByTagName("args").item(0);
    }


    public KasperObject constructObject (){
        var nodes = args.getChildNodes();
        var values = nodes.item(1);
        base_value = values.getChildNodes().item(0);
        return recursivelyConstruct(base_value);
    }


    private KasperObject recursivelyConstruct (Node currNode){
        var type = currNode.getNodeName();
        if (type.equals("string")) {
            return new KasperString(currNode.getTextContent());
        }
        else if (type.equals("list")){
            var list = currNode.getChildNodes();
            var objList = new KasperList();
            for (int i = 0; i<list.getLength(); i++){
                objList.addToList(recursivelyConstruct(list.item(i)));
            } return objList;
        }

        var list = currNode.getChildNodes();
        var objMap = new KasperMap();
        for (int i=0; i<list.getLength(); i+=2){
            var keyNode = list.item(i+1);
            var valueNode = keyNode.getChildNodes().item(0);
            var keyName = list.item(i);
            objMap.put(keyName.getTextContent(), recursivelyConstruct(valueNode));
        } return objMap;
    }
}