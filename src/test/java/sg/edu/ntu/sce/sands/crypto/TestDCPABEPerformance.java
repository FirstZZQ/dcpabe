package sg.edu.ntu.sce.sands.crypto;

import java.math.BigInteger;
import java.util.Random;
import java.util.Vector;
import sg.edu.ntu.sce.sands.crypto.PerformanceUtils;
import sg.edu.ntu.sce.sands.crypto.dcpabe.AuthorityKeys;
import sg.edu.ntu.sce.sands.crypto.dcpabe.Ciphertext;
import sg.edu.ntu.sce.sands.crypto.dcpabe.DCPABE;
import sg.edu.ntu.sce.sands.crypto.dcpabe.GlobalParameters;
import sg.edu.ntu.sce.sands.crypto.dcpabe.Message;
import sg.edu.ntu.sce.sands.crypto.dcpabe.PersonalKeys;
import sg.edu.ntu.sce.sands.crypto.dcpabe.PublicKeys;
import sg.edu.ntu.sce.sands.crypto.dcpabe.ac.AccessStructure;
import sg.edu.ntu.sce.sands.crypto.AttributeGen;

public class TestDCPABEPerformance {
	
	public enum TestMode {
		ATTRIBUTE,
		POLICY_LEN,
		CLIENT_ATTR_NUM
	}
	
	static int num_rounds = 8000;
	static int num_user_tested = 600;
	static String user_name = "default user";

	static void Test(TestMode mode, int min, int max, int defAttr, int defPol, int defClient){
		
		//a random 192-bit integer as cleartext
		BigInteger cleartext = PerformanceUtils.random(BigInteger.valueOf(65536).pow(12));
		GlobalParameters gp = DCPABE.globalSetup(160);
		
		switch (mode){
		case ATTRIBUTE:
			for (int i=min; i<=max; i++){
				subTest(i, defPol, defClient, num_rounds, gp);
			}
			break;
		case POLICY_LEN:
			for (int i=min; i<=max; i++){
				subTest(defAttr, i, defClient, num_rounds, gp);
			}
			break;
		case CLIENT_ATTR_NUM:
			for (int i=min; i<=max; i++){
				subTest(defAttr, defPol, i, num_rounds, gp);
			}
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void subTest(int total_attr_num, int attr_pol_num, int client_attr_num, int pass_num, GlobalParameters gp) {
		
		System.out.println("Attribute number="+total_attr_num+", total attributes="+attr_pol_num+", client attribute number="+client_attr_num+", number of run="+pass_num);
		
		int internal_pass=3;
		
		AttributeGen attgen=new AttributeGen();
		Vector<String> formula_group = attgen.gen(total_attr_num, attr_pol_num, pass_num);
		String [] formula_array = new String[pass_num];		//all boolean formula tested
		formula_group.toArray(formula_array);
		
		double time;
		
		int counter=0;
		PersonalKeys [] attr_array = new PersonalKeys [num_user_tested];	//each element contains all attributes the user has
		int attr_client=Math.min(client_attr_num, total_attr_num);
		long start, end, oldtime=0, newtime=0;
		
		Vector<String> attr_list = (Vector<String>) attgen.backup_attrlist.clone();
		Random rnd = new Random();
		
		//authority setup
		AuthorityKeys ak = DCPABE.authoritySetup("default Authority", 
				gp, 
				(String [])attgen.backup_attrlist.toArray(new String[]{}));
		
		//private key generation
		for (int i=0; i<num_user_tested; i++){
			attr_list = (Vector<String>) attgen.backup_attrlist.clone();
			attr_array[i] = new PersonalKeys(user_name);
			for (int j=0; j<attr_client; j++){
				attr_array[i].addKey(DCPABE.keyGen(user_name, 
						attr_list.remove(rnd.nextInt(attr_list.size())), 
						ak.getSecretKeys().get("default Authority"), 
						gp));
			}
		}
		
		System.gc();
		
		//test encryption
		do{
			start=System.nanoTime();
			for (int j=0; j<internal_pass; j++){
				for (String i:formula_array){
					AccessStructure arho = AccessStructure.buildFromPolicy(i);
					Message m = new Message();
					PublicKeys pks = new PublicKeys();
					pks.subscribeAuthority(ak.getPublicKeys());
					Ciphertext ct = DCPABE.encrypt(m, arho, gp, pks);
				}
			}
			end=System.nanoTime();
			oldtime=newtime;
			newtime=end-start;
		}while (((double)Math.abs(newtime-oldtime)) / (double)newtime > 0.02);
	
		time=(((double)end-(double)start)/1000000000);
		
		System.out.println("\tEncryption Time="+PerformanceUtils.formatNumber(time/(double)pass_num/((double)internal_pass), 12)+"s");
		//System.out.println(PerformanceUtils.formatNumber(time/(double)pass_num/((double)internal_pass), 10)+", ");
		
		System.gc();
	}
	
}
