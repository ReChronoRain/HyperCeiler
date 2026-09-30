import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import com.sevtinge.hyperceiler.Application;
import com.sevtinge.hyperceiler.utils.XposedActivateHelper;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import fan.appcompat.app.AlertDialog;
public class ActivationDialogTest {
    static int passed,failed;
    static Activity current;
    static void event(String method,Object arg){try{if(arg==null)XposedActivateHelper.class.getMethod(method).invoke(null);else XposedActivateHelper.class.getMethod(method,Activity.class).invoke(null,arg);}catch(NoSuchMethodException oldVersion){}catch(Exception e){throw new AssertionError(e);}}
    static Activity reset(){if(current!=null)event("clear",current);Application.isModuleActivated=true;event("onActivationChanged",null);Handler.advance(0);Handler.reset();AlertDialog.reset();current=new Activity();return current;}
    static void expect(boolean ok){if(!ok)throw new AssertionError();}
    static void test(String name,Runnable test){try{test.run();passed++;System.out.println("PASS "+name);}catch(AssertionError e){failed++;System.out.println("FAIL "+name);}}
    public static void main(String[] args){
        test("active startup has no warning or polling",()->{Activity a=reset();XposedActivateHelper.init(a);Handler.advance(20000);expect(AlertDialog.shown==0&&Handler.pending()==0);});
        test("service binds at 3s, later than old timeout",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);Handler.advance(3000);Application.isModuleActivated=true;event("onActivationChanged",null);Handler.advance(20000);expect(AlertDialog.shown==0&&Handler.pending()==0);});
        test("genuine inactive warns once after grace",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);XposedActivateHelper.init(a);Handler.advance(9999);expect(AlertDialog.shown==0);Handler.advance(1);expect(AlertDialog.shown==1);XposedActivateHelper.init(a);Handler.advance(20000);expect(AlertDialog.shown==1);});
        test("late bind dismisses already visible warning",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);Handler.advance(10000);expect(AlertDialog.visible==1);Application.isModuleActivated=true;Looper.worker=true;event("onActivationChanged",null);Looper.worker=false;Handler.advance(0);expect(AlertDialog.visible==0&&Handler.pending()==0);});
        test("background cancels pending warning",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);Handler.advance(1000);event("clear",a);Handler.advance(20000);expect(AlertDialog.shown==0&&Handler.pending()==0);});
        test("destroyed activity never receives dialog",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);a.destroyed=true;Handler.advance(20000);expect(AlertDialog.shown==0);});
        test("rotation cancels old activity and warns only new",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);Handler.advance(1000);event("clear",a);current=new Activity();XposedActivateHelper.init(current);Handler.advance(10000);expect(AlertDialog.shown==1&&DialogHelper.last==current);});
        test("binder death/rebind cancels recovery warning",()->{Activity a=reset();XposedActivateHelper.init(a);Application.isModuleActivated=false;Looper.worker=true;event("onActivationChanged",null);Looper.worker=false;Handler.advance(0);expect(Handler.pending()==1);Handler.advance(500);Application.isModuleActivated=true;event("onActivationChanged",null);Handler.advance(20000);expect(AlertDialog.shown==0&&Handler.pending()==0);});
        test("ignore then resume does not repeatedly warn",()->{Activity a=reset();Application.isModuleActivated=false;XposedActivateHelper.init(a);Handler.advance(10000);event("clear",a);XposedActivateHelper.init(a);Handler.advance(20000);expect(AlertDialog.shown==1&&AlertDialog.visible==0);});
        test("non-activity context never opens UI",()->{reset();Application.isModuleActivated=false;XposedActivateHelper.init(new android.content.Context());Handler.advance(20000);expect(AlertDialog.shown==0);});
        System.out.println("ACTIVATION_TEST: "+passed+" passed; "+failed+" failed");if(failed>0)System.exit(1);
    }
}
