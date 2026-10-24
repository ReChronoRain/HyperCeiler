"""Run the four additional scalar banks using the existing ARM execution harness."""
from pathlib import Path
import sys
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
src=W/'tests/launcher-7751/verify_icon_render_arm.py'
source=src.read_text(encoding='utf8')
old="['hc_icon_width_entry','hc_icon_height_entry','hc_desktop_icon_width_entry','hc_desktop_icon_height_entry','hc_dock_icon_width_entry','hc_dock_icon_height_entry']"
new="['hc_progress_icon_width_entry','hc_progress_icon_height_entry','hc_progress_dock_width_entry','hc_progress_dock_height_entry']"
source=source.replace(old,new)
old="['hc_icon_render_enabled','hc_folder_title_scale_bits','hc_icon_width_hits','hc_icon_height_hits','hc_desktop_icon_width_hits','hc_desktop_icon_height_hits','hc_dock_icon_width_hits','hc_dock_icon_height_hits','hc_icon_width_original','hc_icon_height_original','hc_desktop_icon_width_original','hc_desktop_icon_height_original','hc_dock_icon_width_original','hc_dock_icon_height_original']"
new="['hc_progress_icon_enabled','hc_folder_title_scale_bits','hc_progress_icon_width_hits','hc_progress_icon_height_hits','hc_progress_dock_width_hits','hc_progress_dock_height_hits','hc_progress_icon_width_original','hc_progress_icon_height_original','hc_progress_dock_width_original','hc_progress_dock_height_original']"
source=source.replace(old,new).replace("('arm-'+R.name)","('progress-arm-'+R.name)")
source=source.replace("'Q'*14","'Q'*10").replace("*([0]*6)","*([0]*4)")
source=source.replace('range(6)','range(4)').replace('stop+24','stop+16')
source=source.replace('in [0,2,3]','in [0,1]')
source=source.replace("'Q'*6,u.mem_read(data+16,48)","'Q'*4,u.mem_read(data+16,32)")
source=source.replace('expected=[0]*6','expected=[0]*4').replace('ICON_RENDER_ARM cases=','PROGRESS_ICON_ARM cases=')
source=source.replace('no device execution','no device execution')
exec(compile(source,str(src),'exec'),{'__file__':str(src),'__name__':'__main__'})
