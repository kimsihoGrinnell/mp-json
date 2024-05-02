import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

public class Test {
    public static void main(String[] args) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new FileReader(new File("./StringTest.txt")));
        // string test
        JSONString str = JSON.parseString(reader);
        System.out.println("Print: " + str.toString());
        // number test
        BufferedReader reader2 = new BufferedReader(new FileReader(new File("./IntegerTest.txt")));
        JSONValue num = JSON.parseNegNum(reader2, '0');
        System.out.println("Int test: " + num.toString());
        reader.close();
        // constant test
        BufferedReader reader3 = new BufferedReader(new FileReader(new File("./ConstantTest.txt")));
        JSONConstant cst = JSON.parseConstant(reader3, (int)'t');
        System.out.println("Constant test: " + cst.toString());
        // hash test
        BufferedReader reader4 = new BufferedReader(new FileReader(new File("./HashTest.txt")));
        JSONHash hash = JSON.parseHash(reader4);
        // System.out.println("Hash test: " + hash.toString());

        // final boss
        BufferedReader readerBoss = new BufferedReader(new FileReader(new File("./finalboss.json")));
        JSONHash json = (JSONHash) JSON.parse(readerBoss);
        System.out.println("FINAL BOSS test: " + json.toString());
    }
}
