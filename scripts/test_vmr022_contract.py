#!/usr/bin/env python3
"""VMR-022 static regression gate. Does not replace OCR tests on Samsung."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
main=(root/'app/src/main/java/fr/visionmaxread/MainActivity.java').read_text()
service=(root/'app/src/main/java/fr/visionmaxread/SpecialistOcrDownloadService.java').read_text()
models=(root/'app/src/main/java/fr/visionmaxread/SpecialistOcrModels.java').read_text()
native=(root/'app/src/main/java/fr/visionmaxread/SpecialistOcrProcess.java').read_text()
for required in (
    'Tester tous les OCR · même photo',
    'requestCompareAll()', 'showBatchResults()', 'lastBatchTicket',
    'requestAutoInstall(missing.toArray(new SpecialistOcrModels.Spec[0]),false,null,true)',
    'if(compareAll)requestCompareAll()', 'new OcrService().recognize(this,image)',
    'specialistOcr.recognize(this,spec,image)',
    'SpecialistOcrModels.LIGHTON,SpecialistOcrModels.SURYA',
    'batchOcrActive.compareAndSet(false,true)', 'batchOcrCancel.set(true)',
    'sessions.documentCurrent(ticket)', 'showComparisonText(r.engine,r.text,ticket)',
    'Nemotron 3.5 Parse · portage Android encore nécessaire',
):
    assert required in main, required
assert main.count('specialistOcr.recognize(this,spec,image)')==1
assert 'newSingleThreadExecutor()' in main and 'specialistWorker.execute' in main
assert 'SpecialistOcrAutoInstaller.ensure' in service
# Android AlertDialog only guarantees clickable list items when no message view hides the list.
for start,end in (('private void showSpecialistActions(', 'private void confirmDeleteModel('),
                  ('private void showBatchResults(', 'private void showOcrEngines(')):
    section=main.split(start,1)[1].split(end,1)[0]
    assert '.setItems(' in section, 'selection UI missing: '+start
    assert section.find('.setMessage(')<0 or section.index('.setItems(')<section.index('.setMessage('), 'message masks selectable list: '+start
assert 'Nemotron' not in service and 'NEMOTRON' not in models
assert 'SpecialistOcrModels.verified' in native
print('VMR-022 static batch contract OK; physical OCR, CPU memory and CER/WER remain unverified')
