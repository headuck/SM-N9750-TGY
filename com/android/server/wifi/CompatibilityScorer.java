package com.android.server.wifi;

import com.android.server.wifi.WifiCandidates;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.util.Collection;

final class CompatibilityScorer implements WifiCandidates.CandidateScorer {
    public static final int BAND_5GHZ_AWARD_IS_40 = 40;
    public static final int COMPATIBILITY_SCORER_DEFAULT_EXPID = 42504592;
    public static final int CURRENT_NETWORK_BOOST_IS_16 = 16;
    public static final int LAST_SELECTION_AWARD_IS_480 = 480;
    public static final int RSSI_SCORE_OFFSET = 85;
    public static final int RSSI_SCORE_SLOPE_IS_4 = 4;
    public static final int SAME_BSSID_AWARD_IS_24 = 24;
    public static final int SECURITY_AWARD_IS_80 = 80;
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final ScoringParams mScoringParams;

    CompatibilityScorer(ScoringParams scoringParams) {
        this.mScoringParams = scoringParams;
    }

    public String getIdentifier() {
        return WifiNetworkSelector.PRESET_CANDIDATE_SCORER_NAME;
    }

    private WifiCandidates.ScoredCandidate scoreCandidate(WifiCandidates.Candidate candidate) {
        int score;
        int score2 = (Math.min(candidate.getScanRssi(), this.mScoringParams.getGoodRssi(candidate.getFrequency())) + 85) * 4;
        if (candidate.getFrequency() >= 5000) {
            score2 += 40;
        }
        int score3 = score2 + ((int) (candidate.getLastSelectionWeight() * 480.0d));
        if (candidate.isCurrentNetwork()) {
            score3 += 40;
        }
        if (!candidate.isOpenNetwork()) {
            score3 += 80;
        }
        if (OpBrandingLoader.Vendor.ATT != mOpBranding) {
            score = score3 - (candidate.getEvaluatorId() * 1000);
        } else if (!candidate.isOpenNetwork() || candidate.getEvaluatorId() >= 2) {
            score = score3 - (candidate.getEvaluatorId() * 1000);
        } else {
            score = score3 - 3000;
        }
        return new WifiCandidates.ScoredCandidate(((double) score) + (((double) candidate.getScanRssi()) / 1000.0d), 10.0d, candidate);
    }

    public WifiCandidates.ScoredCandidate scoreCandidates(Collection<WifiCandidates.Candidate> group) {
        WifiCandidates.ScoredCandidate choice = WifiCandidates.ScoredCandidate.NONE;
        for (WifiCandidates.Candidate candidate : group) {
            WifiCandidates.ScoredCandidate scoredCandidate = scoreCandidate(candidate);
            if (scoredCandidate.value > choice.value) {
                choice = scoredCandidate;
            }
        }
        return choice;
    }

    public boolean userConnectChoiceOverrideWanted() {
        return true;
    }
}
