/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person;

import ir.ac.ut.iis.person.evaluation.Evaluator;
import ir.ac.ut.iis.person.evaluation.aspire.ASPIREEvaluator;
import ir.ac.ut.iis.person.evaluation.person.PERSONEvaluator;

/**
 *
 * @author shayan
 */
public class Configs {

    public static RunStage runStage = RunStage.NORMAL;

    // General parameters
    public static final int numOfResults = 100;     // Number of results retrieved
    public static boolean InappropriateQueriesHeuristic = true;     // Whether to use inappropriate queries heuristic or not
    public static int skipQueries = 13_000;        // Skip this number of queries from the list of queries
    public static int queryCount = 2_000;       // The number of queries to consider
    public static int ndcgAt = 100;             // k=ndcgAt for calculating NDCG@k
    public static final boolean useDFCache = true;      // Just keep it unchanged
    public static boolean useSearchCaching = true;   // See the comments for ignoreSelfCitations
    public static final int randomSeed = 1;     // Random seed
    public static boolean multiThreaded = true;

    // ASPIRE parameters    
    public static final double ASPIRECoefficient = 2;    // topkRel=ASPIRECoefficient*topkEval in ASPIRE
    public static final String badDocumentsFileName = "badDocuments.txt";   // Just keep it unchanged       

    // Fundamental parameters
    public static String datasetName = "aminer_>2002";     // The name of the dataset folder
    public static String datasetRoot = getDatasetRoot();      // The root address of the dataset
    public static String database_name = "aminer_>2002";     // The MySQL database name
    public static String databaseTablesPostfix = "";    // Just keep it unchanged
    public static String queryField = "title";          // The field of the query paper used as query. Keep it unchanged
    public static int mapBias = 0;                      // Just keep it unchanged
    public static boolean dontUseMergedPapersAsQuery = true;        // indicating whether merged papers are used as query papers or not. Keep it unchanged
    public static String baseSimilarityName = "LM";     // What base similarity to use: LM, MyLM, or TFIDF. MyLM is our own implementation of LM according
    // to the paper "A Study of Smoothing Methods for Language Models Applied to Information Retrieval".
    // Don't use MyLM with Campos methods since it produces negative retrieval scores which can make a problem in IRR and SRR
    public static float lmDirichletMu = 100f;           // The Mu parameter of LM and MyLM methods
    public static Evaluator evaluator = new PERSONEvaluator();//new MethodBasedEvaluator("queries-agg-100.txt");      // Indicates whether to use PERSON, ASPIRE, or MethodBasedEvaluator for evaluation
//    public static Evaluator evaluator = null;
    public static int runNumber = 176;                  // Just a number so one can execute different runs without collision in the files or folders
    public static String indexName = "index_PPR";      // The name of the index folder
    public static String queryFile = "test.txt";     // The name of the queries file

    // PERSON parameters
    public static boolean ignoreSelfCitations = false;      // Indicates whether ignore self-citations heuristic is used or not. When true set useSearchCaching=false
    public static boolean yearFiltering = true;        // Whether to use publication-date-based filtering or not. Recommended to be false when using ASPIRE and true when using PERSON

    // Profile  parameters
    public static int onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 0;        // Only consider papers whose first authors have more than this number of papers as query papers
    public static int onlyQueriesWhoseAuthorHasLessThan_THIS_Papers = -1;       // Only consider papers whose first authors have less than this number of papers as query papers. Use -1 for no limitation
    public static boolean ignoreQueryDocumentInQueryExpansion = true;           // Ignore the query paper in extracting the profile of the searcher (First author of the paper). Recommended to be true with onlyQueriesWhoseAuthorsHaveMoreThanOnePaper > 0 when algorithms use profile information
    public static boolean useLogTFInProfilesCreation = false;                   // Whether to use log(TF) instead of TF in extracting user profile
    public static boolean useTFIDFWeightingInCampos = false;                    // Whether to use TF-IDF instead of IDF in extracting user profile
    public static boolean dontUseAuthorsRelevantPapersInProfile = true;     // Whether to use author's relevant papers in profile or not. Recommended to be used when ignoreSelfCitations=false

    // Clustered parameters
    public static String graphFile = "authors_giant_graph.csv";     // The social network graph file
    public static boolean useCachedPPRs = false;            // Just keep it unchanged
    public static boolean ignoreTopLevelCluster = true;     // Ignore the top-level cluster in cluster hierarchy (used when the top-level cluster includes all the nodes for performance improvement)
    public static boolean loadGraph = false;                // Just keep it unchanged
    public static double pagerankAlpha = 0.15;              // the Alpha value in PageRank calculation
    public static String clustersFileName = null;     // Keep it unchanged
    public static int pruneThreshold = 1000;            // -1 for not pruning

    // SocialTextual parameters
    public static int socialTextualDegree = 2;        // Keep it unchanged. Use friends and friends-of-friends in Social-Textual
    public static double selfConsiderConstant = 0.;   // If 0., do not consider the searcher himself in scoring. Otherwise, consider a User Relatedness (urf) of 1/selfConsiderConstant for the searcher. May be overriden in CiteseerxSocialTextualValueSource constructor.
    public static boolean penalizeMultipleAuthorsInSocialTextual = true;      // Penalize the contribution of an author in a paper when there are more authors
    public static boolean considerFriendsOfFriendsInRandomNeighborFilteringSearcher = false;    // Just ignore it
    public static float ratioOfCandidateToResults = 2.f;        // Just ignore it

    // Topic parameters
    public static boolean useCachedTopics = true;       // Keep it true
    public static String topicsName = "15_SymmetricAlpha";      // The name of the topics folder
    public static String profileTopicsDBTable = "_PPR";        // Just ignore it
    public static boolean myCosineWeightType = true;        // Just ignore it
    public static boolean removeCurrentPaperFromAuthorTopics = true;       // Ignore current paper's topics in author's topics. Not necessarily implemented completely
    public static boolean useDirichletEstimationForAuthorTopics = false;    // Just ignore it

    public static String generalParameters() {
        return "NOR=" + numOfResults + ",IQH=" + InappropriateQueriesHeuristic + ",SQ=" + skipQueries + ",QC=" + queryCount + ",NDCGAT=" + ndcgAt + ",USC=" + useSearchCaching;
    }

    public static String aspireParameters() {
        return "TkR=" + numOfResults * ASPIRECoefficient + ",TN=" + topicsName + ",BDFN=" + badDocumentsFileName;
    }

    public static String fundamentalParameters() {
        return "EP=" + evaluator.getName() + ",RN=" + runNumber + ",QF=" + queryFile + ",BS=" + baseSimilarityName + ",LMDMU=" + lmDirichletMu + ",QF=" + queryField + ",MB=" + mapBias + ",DUMPAS=" + dontUseMergedPapersAsQuery;
    }

    public static String commonParameters() {
        return fundamentalParameters() + "," + generalParameters() + (temporaryParameters() == null ? "" : "," + temporaryParameters());
    }

    public static String commonParametersWithoutEvaluator() {
        return "RN=" + runNumber + ",BS=" + baseSimilarityName + ",LMDMU=" + lmDirichletMu + ",QF=" + queryField + ",MB=" + mapBias + ",DUMPAS=" + dontUseMergedPapersAsQuery + "," + generalParameters() + (temporaryParameters() == null ? "" : "," + temporaryParameters());
    }

    public static String personParameters() {
        return "ISC=" + ignoreSelfCitations + ",YF=" + yearFiltering;
    }

    public static String personDefaultParameters() {
        return "ISC=" + ignoreSelfCitations + ",YF=" + true;
    }

    public static String profileParameters() {
        return "OQWAHMTP=" + onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers + ",OQWAHLTP=" + onlyQueriesWhoseAuthorHasLessThan_THIS_Papers + ",RCPFAP=" + removeCurrentPaperFromAuthorTopics + ",IQDIQE=" + ignoreQueryDocumentInQueryExpansion + ",ULTFIPC=" + useLogTFInProfilesCreation + ",UTFIDFWIC=" + useTFIDFWeightingInCampos + ",DUARPIP=" + dontUseAuthorsRelevantPapersInProfile;
    }

    public static String clusteredParameters() {
        return "Clustered[UCPPR=" + useCachedPPRs + ",ITLC=" + ignoreTopLevelCluster + ",LC=" + loadGraph + ",PRA=" + pagerankAlpha + ",CFN=" + clustersFileName + ",GF=" + graphFile.substring(graphFile.lastIndexOf('/') + 1) + "]";
    }

    public static String temporaryParameters() {
        return null;//"OQWAHMTP=" + onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers;
    }

    public static String socialTextualParameters() {
        return "SocialTextual[KD=" + socialTextualDegree + ",PMAIK=" + penalizeMultipleAuthorsInSocialTextual + ",CFOFIRNFS=" + considerFriendsOfFriendsInRandomNeighborFilteringSearcher + ",ROFTR=" + ratioOfCandidateToResults + ",SCC=" + selfConsiderConstant + "]";
    }

    public static String topicParameters() {
        return "Topic[TN=" + topicsName + ",UCT=" + useCachedTopics + ",PTDT=" + profileTopicsDBTable + ",MCWT=" + myCosineWeightType + "]";
    }

    public static void initializeWithASPIRE() {
        Configs.evaluator = new ASPIREEvaluator(datasetRoot + "topics/" + topicsName + "/clusters.txt", datasetRoot + "topics/" + topicsName + "/profiles.txt", (int) (Configs.numOfResults * Configs.ASPIRECoefficient));
        Configs.onlyQueriesWhoseAuthorHasMoreThan_THIS_Papers = 0;
        Configs.yearFiltering = false;
    }

    public static String getDatasetRoot() {
        return "../datasets/" + datasetName + "/";
    }

    public enum RunStage {
        NORMAL,
        CREATE_INDEXES,
        CREATE_INDEXES_WITH_TOPICS_STEP1,
        CREATE_INDEXES_WITH_TOPICS_STEP2,
        CREATE_INDEXES_WITH_TOPICS_STEP3,
        CREATE_INDEXES_WITH_PPRs,
        CREATE_QUERIES,
        CREATE_SOCIAL_TEXTUAL_DATABASE,
        CREATE_METHOD_BASED_JUDGMENTS;
    }

    private Configs() {
    }

}
