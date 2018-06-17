/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.datasets.citeseerx.old;

import ir.ac.ut.iis.person.Configs;
import static ir.ac.ut.iis.person.datasets.citeseerx.old.GraphExtractor.readWeights;
import ir.ac.ut.iis.retrieval_tools.citeseerx.PapersReader;
import ir.ac.ut.iis.retrieval_tools.domain.MyIterable;
import ir.ac.ut.iis.retrieval_tools.papers.BasePaper;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class MultiDisciplinaryAuthorExtractor implements MyIterable<BasePaper> {

    Map<Integer, Map<Integer, Integer>> map = new TreeMap<>();
    Map<Integer, List<String>> weights = readWeights(Configs.datasetRoot+"graph-weights.txt", 3);

    public static void main(String[] args) {
        MultiDisciplinaryAuthorExtractor multiDisciplinaryAuthorExtractor = new MultiDisciplinaryAuthorExtractor();
        PapersReader papersReader = new PapersReader(multiDisciplinaryAuthorExtractor);
        papersReader.run(Configs.datasetRoot + "papers_giant.txt");
        System.out.println("Reading papers done");
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("multidisciplinary-authors-not-web.txt")))) {
            for (Map.Entry<Integer, Map<Integer, Integer>> e : multiDisciplinaryAuthorExtractor.map.entrySet()) {
                Map<Integer, Integer> m = e.getValue();
                int count = 0;
                boolean check = false;
                int webCount = 0;
                int otherCount = 0;
                for (Map.Entry<Integer, Integer> v : m.entrySet()) {
                    if (v.getValue() > 5 && !v.getKey().equals(9)) {
                        if (v.getKey().equals(0)) {
                            check = true;
                            webCount = v.getValue();
                        } else {
                            if (otherCount < v.getValue()) {
                                otherCount = v.getValue();
                            }
                        }
                        count++;
                    }
                }
                if (count >= 2 && !check) {
                    writer.write(e.getKey() + " " + count + "\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MultiDisciplinaryAuthorExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int count = 0;

    @Override
    public boolean doAction(BasePaper d) {
        List<String> g = weights.get((int) d.getId());
        if (g == null) {
            System.out.println("!! " + d.getId());
            return true;
        }
        for (String get : g) {
            int superTopic = getSuperTopic(get);
            for (String author : d.getUnprocessableRefs()) {
                Integer a = Integer.valueOf(author);
                Map<Integer, Integer> get1 = map.get(a);
                if (get1 == null) {
                    get1 = new TreeMap<>();
                    map.put(a, get1);
                }
                Integer get2 = get1.get(superTopic);
                if (get2 == null) {
                    get2 = 0;
                }
                get1.put(superTopic, get2 + 1);
            }
        }
        count++;
        if (count % 10_000 == 0) {
            System.out.println(count);
        }
        return true;
    }

    public static int getSuperTopic(String topic) {
        switch (topic) {
            case "Web searching and information discovery":
            case "Specialized information retrieval":
            case "Users and interactive retrieval":
            case "Multimedia information systems":
            case "Retrieval tasks and goals":
            case "Information integration":
            case "Retrieval models and ranking":
            case "Web mining":
            case "Document representation":
            case "Document searching":
            case "Evaluation of retrieval results":
            case "Information retrieval query processing":
            case "Search engine architectures and scalability":
            case "Query languages":
                return 0;
            case "Usability in security and privacy":
            case "Mobile and wireless security":
            case "Privacy-preserving protocols":
            case "Social network security and privacy":
            case "Social engineering attacks":
            case "Software security engineering":
            case "Mathematical foundations of cryptography":
            case "Symmetric cryptography and hash functions":
            case "Management and querying of encrypted data":
            case "Pseudonymity, anonymity and untraceability":
            case "Cryptographic primitives":
            case "Digital cash":
            case "Distributed systems security":
            case "Malware and its mitigation":
            case "Network security":
            case "Security protocols":
            case "Access control":
            case "Cryptanalysis and other attacks":
            case "Data anonymization and sanitization":
            case "Firewalls":
            case "Hardware attacks and countermeasures":
            case "Hardware reverse engineering":
            case "Network privacy and anonymity":
            case "Operating systems security":
            case "Security requirements":
            case "Tamper-proof and tamper-resistant designs":
            case "Authentication":
            case "Authorization":
            case "Browser security":
            case "Cryptographic protocols":
            case "Denial-of-service attacks":
            case "Digital rights management":
            case "Domain-specific security and privacy architectures":
            case "Formal security models":
            case "Hardware security implementation":
            case "Information-theoretic techniques":
            case "Intrusion detection systems":
            case "Key management":
            case "Network forensics":
            case "File system security":

            case "Storage architectures":
            case "Programmable networks":
            case "Record storage systems":
            case "Serial architectures":
            case "Session protocols":
            case "Software reverse engineering":
            case "Calculus":
            case "Computing platforms":
            case "External storage":
            case "General programming languages":
            case "Secondary storage organization":
            case "Solvers":

                return 1;
            case "Simulation types and techniques":
            case "Systems biology":
            case "Visualization design and evaluation methods":
            case "Visualization application domains":
            case "Simulation theory":
            case "Visualization techniques":
            case "Rendering":
            case "Empirical studies in visualization":
            case "Genomics":
            case "Shape modeling":
            case "Computational biology":
            case "Metabolomics metabonomics":
            case "Model development and analysis":
            case "Quantum complexity theory":
            case "Software functional properties":
            case "Animation":
            case "Bioinformatics":
            case "Genetics":
            case "Investigation techniques":
            case "Quantum computation theory":
            case "Signal processing systems":
            case "Visualization systems and tools":
            case "Visualization theory, concepts and paradigms":
            case "Astronomy":
            case "Biology-related information processing":
            case "Computer algebra systems":
            case "Computer vision":
            case "Control methods":
            case "Database administration":
            case "Distribution functions":
            case "Document capture":
            case "Empirical studies":
            case "Evidence collection, storage and analysis":
            case "Functional analysis":
            case "Mathematical optimization":
            case "Physics":
            case "Planning and scheduling":
            case "Presentation protocols":
            case "Probabilistic computation":
            case "Quadrature":
            case "Representation of mathematical objects":
            case "Scanners":
            case "Search methodologies":
            case "Surveillance mechanisms":
            case "Computing education":
            case "Data structures design and analysis":
            case "Document preparation":
            case "Law":
            case "Random network models":
            case "Redundancy":
            case "Safety critical systems":
            case "Streaming models":
            case "Surveillance":
                return 2;
            case "Systems and tools for interaction design":
            case "Ubiquitous and mobile computing systems and tools":
            case "Ubiquitous and mobile devices":
            case "Empirical studies in collaborative and social computing":
            case "Interaction design process and methods":
            case "Ubiquitous and mobile computing design and evaluation methods":
            case "Publishing":
            case "HCI design and evaluation methods":
            case "Interaction design theory, concepts and paradigms":
            case "Learning management systems":
            case "Collaborative and social computing theory, concepts and paradigms":
            case "HCI theory, concepts and models":
            case "Learning settings":
            case "Collaborative and social computing design and evaluation methods":
            case "Distance learning":
            case "Interactive systems and tools":
            case "Interaction techniques":
            case "Accessibility technologies":
            case "Collaborative and social computing devices":
            case "Collaborative learning":
            case "Computer-managed instruction":
            case "Empirical studies in interaction design":
            case "History of computing":
            case "Interaction paradigms":
            case "Interactive learning environments":
            case "Management of computing and information systems":
            case "Media arts":
            case "Philosophical theoretical foundations of artificial intelligence":
            case "Psychology":
            case "Sound and music computing":
            case "Tactile and hand-based interfaces":
            case "Accessibility systems and tools":
            case "Accessibility theory, concepts and paradigms":
            case "Archaeology":
            case "Architecture (buildings)":
            case "Collaborative and social computing systems and tools":
            case "Computer-assisted instruction":
            case "Computing profession":
            case "Design":
            case "E-learning":
            case "Emerging interfaces":
            case "Empirical studies in HCI":
            case "Empirical studies in ubiquitous and mobile computing":
            case "Fine arts":
            case "Interaction devices":
            case "Personal computers and PC applications":
            case "Sound-based input output":
            case "Accessibility design and evaluation methods":
            case "Anthropology":
            case "Cartography":
            case "Consumer products":
            case "Designing software":
            case "Distributed artificial intelligence":
            case "Graphics systems and interfaces":
            case "Language translation":
            case "Location based services":
            case "Mobile information processing systems":
            case "Performing arts":
            case "Robotics":
            case "Storage replication":
            case "System forensics":
            case "System on a chip":
            case "Ubiquitous and mobile computing theory, concepts and paradigms":
            case "Enterprise ontologies, taxonomies and vocabularies":
            case "Natural language processing":
                return 3;
            case "VLSI system specification and constraints":
            case "Real-time system architecture":
            case "VLSI design manufacturing considerations":
            case "Hardware reliability":
            case "Economics of chip design and manufacturing":
            case "VLSI packaging":
            case "Power estimation and optimization":
            case "Processors and memory architectures":
            case "Timing analysis":
            case "Full-custom circuits":
            case "High-level and register-transfer level synthesis":
            case "Parallel programming languages":
            case "On-chip resource management":
            case "Reconfigurable logic and FPGAs":
            case "Analog, mixed-signal and radio frequency test":
            case "Board- and system-level test":
            case "Embedded systems security":
            case "Modeling and parameter extraction":
            case "Network on chip":
            case "Reversible logic":
            case "Test-pattern generation and fault simulation":
            case "Thermal issues":
            case "Digital switches":
            case "Energy generation and storage":
            case "Hardware description languages and compilation":
            case "Interconnect":
            case "Logic synthesis":
            case "Methodologies for EDA":
            case "Parallel algorithms":
            case "Real-time languages":
            case "Real-time operating systems":
            case "3D integrated circuits":
            case "Analog and mixed-signal circuits":
            case "Analysis and design of emerging devices and systems":
            case "Application-specific VLSI designs":
            case "Circuit substrates":
            case "Concurrency":
            case "Concurrent programming languages":
            case "Design for manufacturability":
            case "Design for testability":
            case "Design reuse and communication-based design":
            case "Design rules":
            case "Displays and imagers":
            case "Electro-mechanical devices":
            case "Electromagnetic interference and compatibility":
            case "Embedded systems":
            case "Emerging optical and photonic technologies":
            case "Functional verification":
            case "Hardware reliability screening":
            case "Logic circuits":
            case "Memory test and repair":
            case "Network reliability":
            case "Physical design (EDA)":
            case "Plasmonics":
            case "Quantum technologies":
            case "Real-time system specification":
            case "Semiconductor memory":
            case "Sensors and actuators":
            case "Spintronics and magnetic technologies":
            case "Algorithm design techniques":
            case "Buses and high-speed links":
            case "Compilers":
            case "Concurrent algorithms":
            case "Cyber-physical networks":
            case "Defect-based test":
            case "Electromechanical systems":
            case "Energy distribution":
            case "Event-driven architectures":
            case "Memory and dense storage":
            case "On-chip sensors":
            case "PCB design and layout":
            case "Parallel architectures":
            case "Post-manufacture validation and debug":
            case "Printers":
            case "Program constructs":
            case "Sociology":
            case "Standard cell libraries":
            case "Biographies":
            case "Information storage technologies":
                return 4;
            case "Wireless integrated network sensors":
            case "Wireless access networks":
            case "Network layer protocols":
            case "Overlay and other logical network structures":
            case "Sensor networks":
            case "Other architectures":
            case "Wired access networks":
            case "Mobile networks":
            case "Wireless access points, base stations and infrastructure":
            case "Fault-tolerant network topologies":
            case "Packet-switching networks":
            case "Physical links":
            case "Data path algorithms":
            case "Link-layer protocols":
            case "Network manageability":
            case "Network protocol design":
            case "Electronics":
            case "IT architectures":
            case "Network management":
            case "Network measurement":
            case "Network performance analysis":
            case "Network performance modeling":
            case "Network range":
            case "Wireless devices":
            case "Application layer protocols":
            case "Chemistry":
            case "Control path algorithms":
            case "Cross-layer protocols":
            case "Data center networks":
            case "Distributed algorithms":
            case "End nodes":
            case "Engineering":
            case "Home networks":
            case "In-network processing":
            case "Intermediate nodes":
            case "Logical nodes":
            case "Naming and addressing":
            case "Network design principles":
            case "Network dynamics":
            case "Network experimentation":
            case "Network mobility":
            case "Network simulations":
            case "Network structure":
            case "Networking hardware":
            case "OAM protocols":
            case "Transport protocols":
            case "Web services":
            case "Ad hoc networks":
            case "Data recovery":
            case "Forecasting":
            case "Network File System (NFS) protocol":
            case "Network access control":
            case "Network monitoring":
            case "Protocol correctness":
            case "Public key (asymmetric) techniques":
            case "Sensor applications and deployments":
            case "Sensor devices and platforms":
            case "Storage area networks":
            case "Transportation":
            case "Aerospace":
            case "Algorithmic game theory and mechanism design":
            case "Availability":
            case "Coding theory":
            case "Distributed architectures":
            case "Fault tolerance":
            case "Mathematical software performance":
            case "Programming interfaces":
                return 5;
            case "Software post-development issues":
            case "Software verification and validation":
            case "Enterprise computing infrastructures":
            case "Enterprise modeling":
            case "Software configuration management and version control systems":
            case "Software maintenance tools":
            case "Collaboration in software development":
            case "Computing and business":
            case "Computing in government":
            case "Development frameworks and environments":
            case "Enterprise architectures":
            case "Enterprise information systems":
            case "Maintainability and maintenance":
            case "Service-oriented architectures":
            case "Simulation support systems":
            case "Software development process management":
            case "Agriculture":
            case "Business process management":
            case "Business rules":
            case "Business-IT alignment":
            case "Cloud computing":
            case "Computing industry":
            case "Contextual software domains":
            case "Database design and models":
            case "Database management system engines":
            case "Earth and atmospheric sciences":
            case "Extra-functional properties":
            case "Simulation evaluation":
            case "Software development techniques":
            case "Context specific languages":
            case "Enterprise data management":
            case "Fault models and test metrics":
            case "Impact on the environment":
            case "Middleware for databases":
            case "Process control systems":
            case "Software libraries and repositories":
            case "Software system structures":
            case "Vulnerability management":
            case "Data structures":
                return 6;
            case "Random walks and Markov chains":
            case "Verification":
            case "Machine learning theory":
            case "Oracles and decision trees":
            case "Problems, reductions and completeness":
            case "Pseudorandomness and derandomization":
            case "Timed and hybrid models":
            case "Topology":
            case "Probabilistic representations":
            case "Random projections and metric embeddings":
            case "Formal language definitions":
            case "Numerical analysis":
            case "Parameterized complexity and exact algorithms":
            case "Interactive proof systems":
            case "Proof theory":
            case "Reliability":
            case "Expander graphs and randomness extractors":
            case "Graph algorithms analysis":
            case "Higher order logic":
            case "Machine learning approaches":
            case "Probabilistic inference problems":
            case "Proof complexity":
            case "Streaming, sublinear and near linear time algorithms":
            case "System description languages":
            case "Approximation algorithms analysis":
            case "Complexity classes":
            case "Evaluation":
            case "Graph theory":
            case "Hoare logic":
            case "Integral equations":
            case "Linear logic":
            case "Statistical paradigms":
            case "Tree languages":
            case "Algebraic complexity theory":
            case "Automated reasoning":
            case "Circuit complexity":
            case "Combinatorics":
            case "Complexity theory and logic":
            case "Computability":
            case "Constraint and logic programming":
            case "Continuous functions":
            case "Cross-validation":
            case "Data mining":
            case "Description logics":
            case "Equational logic and rewriting":
            case "Error-correcting codes":
            case "Finite Model Theory":
            case "Formalisms":
            case "Generating random combinatorial structures":
            case "Grammars and context-free languages":
            case "Knowledge representation and reasoning":
            case "Mathematics and statistics":
            case "Nonlinear equations":
            case "Nonparametric statistics":
            case "Online algorithms":
            case "Program semantics":
            case "Programming logic":
            case "Regular languages":
            case "Separation logic":
            case "Stochastic processes":
            case "Automata extensions":
            case "Communication complexity":
            case "Computational geometry":
            case "Database theory":
            case "Estimation":
            case "Machine learning algorithms":
            case "Multivariate statistics":
            case "Probabilistic algorithms":
            case "Symbolic and algebraic algorithms":
            case "Computational advertising":
            case "Computer-aided manufacturing":
            case "Decision analysis":
            case "Differential equations":
            case "Distributed programming languages":
            case "Information flow control":
            case "Interactive computation":
            case "Learning paradigms":
            case "Metrics":
            case "Physical verification":
            case "Probabilistic reasoning algorithms":
            case "Program reasoning":
            case "Storage management":
            case "Type theory":
                return 7;
            case "Trust frameworks":
            case "Medical information policy":
            case "Race and ethnicity":
            case "Social aspects of security and privacy":
            case "Government technology policy":
            case "Privacy policies":
            case "Intellectual property":
            case "Secure online transactions":
            case "Surveys and overviews":
            case "Validation":
            case "Gender":
            case "Health care information systems":
            case "Network economics":
            case "Online shopping":
            case "Health informatics":
            case "Measurement":
            case "Decision support systems":
            case "Online banking":
            case "Privacy protections":
            case "Public Internet":
            case "Age":
            case "Commerce policy":
            case "Computer crime":
            case "Consumer health":
            case "Economics":
            case "General conference proceedings":
            case "General literature":
            case "Geographic characteristics":
            case "IT governance":
            case "Marketing":
            case "Online auctions":
            case "People with disabilities":
            case "Performance":
            case "Religious orientation":
            case "Sexual orientation":
            case "Censorship":
            case "Computing standards, RFCs and guidelines":
            case "Cultural characteristics":
            case "Database activity monitoring":
            case "E-commerce infrastructure":
            case "Electronic data interchange":
            case "Experimentation":
            case "Industry and manufacturing":
            case "Information accountability and usage control":
            case "Military":
            case "Online advertising":
            case "Reference works":
            case "Statistical software":
            case "Document management":
            case "Economics of security and privacy":
            case "Electronic funds transfer":
            case "Empirical studies in accessibility":
            case "Middle boxes network appliances":
            case "Reference models":
            case "Telecommunications":
                return 8;
//Removed items from class 0:
            case "Verification by model checking":
            case "Web protocol security":
            case "Constructive mathematics":
            case "Image manipulation":
            case "Web application security":
            case "Web applications":
            case "Web interfaces":
            case "Digital libraries and archives":
            case "Image compression":
            case "Modal and temporal logics":
            case "Spatial-temporal systems":
            case "Testing with distributed and parallel systems":
            case "Web data description languages":
            case "Abstract machines":
            case "Abstraction":
            case "Automata over infinite objects":
            case "Logic and verification":
                return 9;
            default:
                System.out.println(topic);
                throw new RuntimeException();
        }
    }
}
