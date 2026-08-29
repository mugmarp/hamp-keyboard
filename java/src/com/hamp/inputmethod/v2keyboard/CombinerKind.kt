package com.hamp.inputmethod.v2keyboard

import com.hamp.inputmethod.event.Combiner
import com.hamp.inputmethod.event.DeadKeyCombiner
import com.hamp.inputmethod.event.combiners.NFCNormalizingCombiner
import com.hamp.inputmethod.event.combiners.DeadKeyPreCombiner
import com.hamp.inputmethod.event.combiners.KoashurCombiner
import com.hamp.inputmethod.event.combiners.KoreanCombiner
import com.hamp.inputmethod.event.combiners.vietnamese.VNICombiner
import com.hamp.inputmethod.event.combiners.vietnamese.VietTelexCombiner
import com.hamp.inputmethod.event.combiners.wylie.WylieCombiner

enum class CombinerKind(val factory: () -> Combiner) {
    DeadKey({ DeadKeyCombiner() }),
    DeadKeyPreCombiner({ DeadKeyPreCombiner() }),
    NFCNormalize({ NFCNormalizingCombiner() }),
    Korean({ KoreanCombiner() }),
    KoreanCombineInitials({ KoreanCombiner(combineInitials = true) }),
    VietTelex( { VietTelexCombiner() }),
    VNI( { VNICombiner() }),
    Wylie({ WylieCombiner() }),
    Koashur({ KoashurCombiner() }),
}